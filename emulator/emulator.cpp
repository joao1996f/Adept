#include "VAdept.h"
#include "verilated.h"
#if VM_TRACE
#include "verilated_vcd_c.h"
#endif
#include <cassert>
#include <cerrno>
#include <cstdio>
#include <cstdlib>
#include <fcntl.h>
#include <fstream>
#include <iomanip>
#include <iostream>
#include <map>
#include <queue>
#include <sstream>
#include <string.h>
#include <sys/mman.h>
#include <time.h>
#include <unistd.h>
#include <vector>

enum SIM_CMD { RESET, STEP, UPDATE, POKE, PEEK, FORCE, GETID, GETCHK, FIN };
const int SIM_CMD_MAX_BYTES = 1024;
const int channel_data_offset_64bw = 4; // Offset from start of channel buffer
                                        // to actual user data in 64bit words.
static size_t gSystemPageSize;

template <class T> struct sim_data_t {
  std::vector<T> resets;
  std::vector<T> inputs;
  std::vector<T> outputs;
  std::vector<T> signals;
  std::map<std::string, size_t> signal_map;

  // Calculate the size (in bytes) of data stored in a vector.
  size_t storage_size(const std::vector<T> vec) {
    int nitems = vec.size();
#ifdef VPI_USER_H
    return nitems * sizeof(T);
#else
    size_t result = 0;
    for (int i = 0; i < nitems; i++) {
      result += vec[i]->get_num_words();
    }
    return result * sizeof(uint64_t);
#endif
  }
};

class channel_t {
public:
#define ROUND_UP(N, S) ((((N) + (S)-1) & (~((S)-1))))

  void init_map() {
    static std::string m_prefix("channel_t::init_map - ");
    // ensure the data is available (a full page worth).
    if (lseek(fd, map_size - 1, SEEK_SET) == -1) {
      perror((m_prefix + "file: " + full_file_path + " seek to end of page")
                 .c_str());
      exit(1);
    }
    if (write(fd, "", 1) == -1) {
      perror((m_prefix + "file: " + full_file_path + " write byte").c_str());
      exit(1);
    }
    if (fsync(fd) == -1) {
      perror((m_prefix + "file: " + full_file_path + " fsync").c_str());
      exit(1);
    }
    channel =
        (char *)mmap(NULL, map_size, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    if (channel == MAP_FAILED) {
      perror((m_prefix + "file: " + full_file_path + " mmap").c_str());
      exit(1);
    }
  }

  channel_t(std::string _file_name, size_t _data_size)
      : file_name(_file_name),
        fd(open(file_name.c_str(), O_RDWR | O_CREAT | O_TRUNC, (mode_t)0600)),
        map_size(ROUND_UP(_data_size + channel_data_offset_64bw * 8,
                          gSystemPageSize)) {
    static std::string m_prefix("channel_t::channel_t: ");
    char *rp = realpath(file_name.c_str(), NULL);
    full_file_path = std::string(rp == NULL ? file_name : rp);
    if (rp != NULL) {
      free(rp);
      rp = NULL;
    }
    if (fd == -1) {
      perror((m_prefix + "file: " + full_file_path + " open").c_str());
      exit(1);
    }
    init_map();
  }

  ~channel_t() {
    munmap((void *)channel, map_size);
    close(fd);
  }

  inline void aquire() {
    channel[1] = 1;
    channel[2] = 1;
    while (channel[0] == 1 && channel[2] == 1)
      ;
  }

  inline void release() { channel[1] = 0; }
  inline void produce() { channel[3] = 1; }
  inline void consume() { channel[3] = 0; }
  inline bool ready() { return channel[3] == 0; }
  inline bool valid() { return channel[3] == 1; }
  inline uint64_t *data() {
    return (uint64_t *)(channel + channel_data_offset_64bw);
  }
  inline char *str() { return ((char *)channel + channel_data_offset_64bw); }
  inline uint64_t &operator[](int i) { return data()[i * sizeof(uint64_t)]; }

private:
  // Dekker's alg for sync
  // channel[0] -> tester
  // channel[1] -> simulator
  // channel[2] -> turn
  // channel[3] -> flag
  // channel[4:] -> data
  char volatile *channel;
  const std::string file_name;
  std::string full_file_path;
  const int fd;
  const size_t map_size;
};

template <class T> class sim_api_t {
public:
  sim_api_t() {
    // This is horrible, but we'd rather not have to generate another .cpp
    // initialization file,
    //  and have all our clients update their Makefiles (if they don't use ours)
    //  to build the simulator.
    if (gSystemPageSize == 0) {
      gSystemPageSize = sysconf(_SC_PAGESIZE);
    }
  }

  virtual ~sim_api_t() {
    delete in_channel;
    delete out_channel;
    delete cmd_channel;
  }

  virtual void tick() {
    static bool is_reset;
    // First, Send output tokens
    while (!send_tokens())
      ;
    if (is_reset)
      start();
    is_reset = false;

    // Next, handle commands from the testers
    bool is_exit = false;
    do {
      size_t cmd;
      while (!recv_cmd(cmd))
        ;
      switch ((SIM_CMD)cmd) {
      case RESET:
        reset();
        is_reset = true;
        is_exit = true;
        break;
      case STEP:
        while (!recv_tokens())
          ;
        step();
        is_exit = true;
        break;
      case UPDATE:
        while (!recv_tokens())
          ;
        update();
        is_exit = true;
        break;
      case POKE:
        poke();
        break;
      case PEEK:
        peek();
        break;
      case FORCE:
        poke(true);
        break;
      case GETID:
        getid();
        break;
      case GETCHK:
        getchk();
        break;
      case FIN:
        finish();
        is_exit = true;
        break;
      default:
        break;
      }
    } while (!is_exit);
  }

protected:
  sim_data_t<T> sim_data;

private:
  channel_t *in_channel;
  channel_t *out_channel;
  channel_t *cmd_channel;

  virtual void reset() = 0;
  virtual void start() = 0;
  virtual void finish() = 0;
  virtual void step() = 0;
  virtual void update() = 0;
  // Consumes input tokens
  virtual size_t put_value(T &sig, uint64_t *data, bool force = false) = 0;
  // Generate output tokens
  virtual size_t get_value(T &sig, uint64_t *data) = 0;
  // Find a signal of path
  virtual int search(std::string &path) { return -1; }
  virtual size_t get_chunk(T &sig) = 0;

  void poke(bool force = false) {
    size_t id;
    while (!recv_cmd(id))
      ;
    T obj = sim_data.signals[id];
    if (!obj) {
      std::cerr << "Cannot find the object of id = " << id << std::endl;
      finish();
      exit(2); // Not a normal exit.
    }
    while (!recv_value(obj, force))
      ;
  }

  void peek() {
    size_t id;
    while (!recv_cmd(id))
      ;
    T obj = sim_data.signals[id];
    if (!obj) {
      std::cerr << "Cannot find the object of id = " << id << std::endl;
      finish();
      exit(2); // Not a normal exit.
    }
    while (!send_value(obj))
      ;
  }

  void getid() {
    std::string path;
    while (!recv_cmd(path))
      ;
    std::map<std::string, size_t>::iterator it = sim_data.signal_map.find(path);
    if (it != sim_data.signal_map.end()) {
      while (!send_resp(it->second))
        ;
    } else {
      int id = search(path);
      if (id < 0) {
        // Issue warning message but don't exit here.
        std::cerr << "Cannot find the object, " << path << std::endl;
      }
      while (!send_resp(id))
        ;
    }
  }

  void getchk() {
    size_t id;
    while (!recv_cmd(id))
      ;
    T obj = sim_data.signals[id];
    if (!obj) {
      std::cerr << "Cannot find the object of id = " << id << std::endl;
      finish();
      exit(2); // Not a normal exit.
    }
    size_t chunk = get_chunk(obj);
    while (!send_resp(chunk))
      ;
  }

  bool recv_cmd(size_t &cmd) {
    cmd_channel->aquire();
    bool valid = cmd_channel->valid();
    if (valid) {
      cmd = (*cmd_channel)[0];
      cmd_channel->consume();
    }
    cmd_channel->release();
    return valid;
  }

  bool recv_cmd(std::string &path) {
    cmd_channel->aquire();
    bool valid = cmd_channel->valid();
    if (valid) {
      path = cmd_channel->str();
      cmd_channel->consume();
    }
    cmd_channel->release();
    return valid;
  }

  bool send_resp(size_t value) {
    out_channel->aquire();
    bool ready = out_channel->ready();
    if (ready) {
      (*out_channel)[0] = value;
      out_channel->produce();
    }
    out_channel->release();
    return ready;
  }

  bool recv_value(T &obj, bool force = false) {
    in_channel->aquire();
    bool valid = in_channel->valid();
    if (valid) {
      put_value(obj, in_channel->data(), force);
      in_channel->consume();
    }
    in_channel->release();
    return valid;
  }

  bool send_value(T &obj) {
    out_channel->aquire();
    bool ready = out_channel->ready();
    if (ready) {
      get_value(obj, out_channel->data());
      out_channel->produce();
    }
    out_channel->release();
    return ready;
  }

  bool recv_tokens() {
    in_channel->aquire();
    bool valid = in_channel->valid();
    if (valid) {
      size_t off = 0;
      uint64_t *data = in_channel->data();
      for (size_t i = 0; i < sim_data.inputs.size(); i++) {
        T &sig = sim_data.inputs[i];
        off += put_value(sig, data + off);
      }
      in_channel->consume();
    }
    in_channel->release();
    return valid;
  }

  bool send_tokens() {
    out_channel->aquire();
    bool ready = out_channel->ready();
    if (ready) {
      size_t off = 0;
      uint64_t *data = out_channel->data();
      for (size_t i = 0; i < sim_data.outputs.size(); i++) {
        T &sig = sim_data.outputs[i];
        off += get_value(sig, data + off);
      }
      out_channel->produce();
    }
    out_channel->release();
    return ready;
  }
};

class VerilatorDataWrapper {
public:
  virtual size_t get_value(uint64_t *values) = 0;
  virtual size_t put_value(uint64_t *values) = 0;
  virtual size_t get_num_words() = 0;
};

class VerilatorCData : public VerilatorDataWrapper {
public:
  VerilatorCData(CData *_signal) { signal = _signal; }

  virtual size_t get_value(uint64_t *values) {
    values[0] = (uint64_t)(*signal);
    return 1;
  }

  virtual size_t put_value(uint64_t *values) {
    uint64_t mask = 0xff;
    *signal = (CData)(mask & values[0]);
    return 1;
  }

  virtual size_t get_num_words() { return 1; }

private:
  CData *signal;
};

class VerilatorSData : public VerilatorDataWrapper {
public:
  VerilatorSData(SData *_signal) { signal = _signal; }

  virtual size_t get_value(uint64_t *values) {
    values[0] = (uint64_t)(*signal);
    return 1;
  }

  virtual size_t put_value(uint64_t *values) {
    uint64_t mask = 0xffff;
    *signal = (SData)(mask & values[0]);
    return 1;
  }

  virtual size_t get_num_words() { return 1; }

private:
  SData *signal;
};

class VerilatorIData : public VerilatorDataWrapper {
public:
  VerilatorIData(IData *_signal) { signal = _signal; }

  virtual size_t get_value(uint64_t *values) {
    values[0] = (uint64_t)(*signal);
    return 1;
  }

  virtual size_t put_value(uint64_t *values) {
    uint64_t mask = 0xffffffff;
    *signal = (IData)(mask & values[0]);
    return 1;
  }

  virtual size_t get_num_words() { return 1; }

private:
  IData *signal;
};

class VerilatorQData : public VerilatorDataWrapper {
public:
  VerilatorQData(QData *_signal) { signal = _signal; }

  virtual size_t get_value(uint64_t *values) {
    values[0] = (uint64_t)(*signal);
    return 1;
  }

  virtual size_t put_value(uint64_t *values) {
    *signal = (QData)values[0];
    return 1;
  }

  virtual size_t get_num_words() { return 1; }

private:
  QData *signal;
};

class VerilatorWData {
public:
  VerilatorWData(WData *_wdatas, size_t _numWdatas) {
    wdatas = _wdatas;
    numWdatas = _numWdatas;
  }

  virtual size_t get_value(uint64_t *values) {
    bool numWdatasEven = (numWdatas % 2) == 0;
    for (int i = 0; i < numWdatas / 2; i++) {
      uint64_t value = ((uint64_t)wdatas[i * 2 + 1]) << 32 | wdatas[i * 2];
      values[i] = value;
    }
    if (!numWdatasEven) {
      values[numWdatas / 2] = wdatas[numWdatas - 1];
    }
    return get_num_words();
  }

  virtual size_t put_value(uint64_t *values) {
    bool numWdatasEven = (numWdatas % 2) == 0;
    for (int i = 0; i < numWdatas / 2; i++) {
      wdatas[i * 2] = values[i];
      wdatas[i * 2 + 1] = values[i] >> 32;
    }
    if (!numWdatasEven) {
      wdatas[numWdatas - 1] = values[numWdatas / 2];
    }
    return get_num_words();
  }

  virtual size_t get_num_words() {
    bool numWdatasEven = numWdatas % 2 == 0;
    if (numWdatasEven) {
      return numWdatas / 2;
    } else {
      return numWdatas / 2 + 1;
    }
  }

private:
  WData *wdatas;
  size_t numWdatas;
};

class Adept_api_t : public sim_api_t<VerilatorDataWrapper *> {
public:
  Adept_api_t(VAdept *_dut) {
    dut = _dut;
    main_time = 0L;
    is_exit = false;
#if VM_TRACE
    tfp = NULL;
#endif
  }

  void init_sim_data() {
    sim_data.inputs.clear();
    sim_data.outputs.clear();
    sim_data.signals.clear();
    sim_data.inputs.push_back(new VerilatorCData(&(dut->io_load_we)));
    sim_data.inputs.push_back(new VerilatorIData(&(dut->io_load_addr_w)));
    sim_data.inputs.push_back(new VerilatorCData(&(dut->io_load_data_in_0)));
    sim_data.inputs.push_back(new VerilatorCData(&(dut->io_load_data_in_1)));
    sim_data.inputs.push_back(new VerilatorCData(&(dut->io_load_data_in_2)));
    sim_data.inputs.push_back(new VerilatorCData(&(dut->io_load_data_in_3)));
    sim_data.outputs.push_back(new VerilatorCData(&(dut->io_success)));
    sim_data.signals.push_back(new VerilatorCData(&(dut->reset)));
    sim_data.signal_map["Adept.reset"] = 0;
  }

#if VM_TRACE
  void init_dump(VerilatedVcdC *_tfp) { tfp = _tfp; }
#endif

  inline bool exit() { return is_exit; }

  virtual inline double get_time_stamp() { return main_time; }

  virtual inline void reset() {
    dut->reset = 1;
    step();
  }

  virtual inline void step() {
    dut->clock = 0;
    dut->eval();
#if VM_TRACE
    if (tfp)
      tfp->dump(main_time);
#endif
    main_time++;
    dut->clock = 1;
    dut->eval();
#if VM_TRACE
    if (tfp)
      tfp->dump(main_time);
#endif
    main_time++;
  }

private:
  VAdept *dut;
  bool is_exit;
  vluint64_t main_time;
#if VM_TRACE
  VerilatedVcdC *tfp;
#endif

  virtual inline size_t put_value(VerilatorDataWrapper *&sig, uint64_t *data,
                                  bool force = false) {
    return sig->put_value(data);
  }

  virtual inline size_t get_value(VerilatorDataWrapper *&sig, uint64_t *data) {
    return sig->get_value(data);
  }

  virtual inline size_t get_chunk(VerilatorDataWrapper *&sig) {
    return sig->get_num_words();
  }

  virtual inline void start() { dut->reset = 0; }

  virtual inline void finish() {
    dut->eval();
    is_exit = true;
  }

  virtual inline void update() { dut->_eval_settle(dut->__VlSymsp); }
};

static Adept_api_t *_Top_api;

double sc_time_stamp() { return _Top_api->get_time_stamp(); }

////////////////////////////////////////////////////////////////////////////////
// Main Function
////////////////////////////////////////////////////////////////////////////////
int main(int argc, char **argv, char **env) {
  // Separate emulator arguments from verilator arguments. For simplicity sake,
  // emulator arguments always come first, and verilator arguments are the
  // remaining non-identifiable arguments.
  // Supported flags:
  // <PROG> --max-cycles=x
  size_t max_cycles = -1;
  std::string program_name = "";
  size_t emu_args = 0;

  // Must provide arguments
  if (argc == 1) {
    std::cerr << "Usage:" << std::endl;
    std::cerr << "\t <PROG> --max-cycles=<MAX_CYCLES> <VERILATOR_FLAGS>" << std::endl;
    exit(1);
  }

  std::vector<std::string> args(argv + 1, argv + argc);
  if (args[0][0] != '-' && args[0][0] != '+') {
    program_name = args[0];
    emu_args++;
  } else {
    std::cerr << "You must provide a program" << std::endl;
    exit(1);
  }

  if (argc > 2 && args[1].find("--max-cycles=") == 0) {
    max_cycles = std::stoi(args[1].c_str() + 13);
    emu_args++;
  }

  std::vector<std::string> verilator_args(argv + 1, argv + argc);
  std::vector<std::string>::const_iterator it;

  Verilated::commandArgs(argc, argv);
  VAdept *top = new VAdept;

  std::string vcdfile = "Adept.vcd";

  for (it = verilator_args.begin(); it != verilator_args.end(); it++) {
    if (it->find("+waveform=") == 0)
      vcdfile = it->c_str() + 10;
  }

#if VM_TRACE
  Verilated::traceEverOn(true);
  VL_PRINTF("Enabling waves..");
  VerilatedVcdC *tfp = new VerilatedVcdC;
  top->trace(tfp, 99);
  tfp->open(vcdfile.c_str());
#endif

  Adept_api_t api(top);
  _Top_api = &api; /* required for sc_time_stamp() */
  api.init_sim_data();

#if VM_TRACE
  api.init_dump(tfp);
#endif
  // Initial Reset for 10 cycles
  for (size_t i = 0; i < 10; i++) {
    api.reset();
  }

  // Load Program TODO
  // Read external elf file and generate an hashmap which is an exact replica of
  // the memory in the processor. Load all addresses to memory. Unused addresses
  // will contain garbage data.
  std::map<size_t, size_t> memory;

  // Set memory write enable to high
  top->io_load_we = 1;
  for (auto const& entry : memory) {
    // Set memory address to write to
    top->io_load_addr_w = entry.first;

    // Load 8 bits at a time and then advance simulation
    top->io_load_data_in_0 = (entry.second & 0x000000ff);
    top->io_load_data_in_1 = (entry.second & 0x0000ff00) >> 8;
    top->io_load_data_in_2 = (entry.second & 0x00ff0000) >> 16;
    top->io_load_data_in_3 = (entry.second & 0xff000000) >> 24;

    // Advance simulation
    api.step();
  }
  // We're done loading data into the memory. Lower write enable and reset
  // processor.
  top->io_load_we = 0;
  for (size_t i = 0; i < 5; i++) {
    api.reset();
  }

  // Processing Program
  while (!top->io_success && sc_time_stamp() < max_cycles) {
    api.step();
  }

#if VM_TRACE
  if (tfp)
    tfp->close();
  delete tfp;
#endif

  delete top;
  exit(0);
}
