#include "VAdept.h"
#include "verilated.h"
#if VM_TRACE
#include "verilated_vcd_c.h"
#endif
#include <cstdio>
#include <cstdlib>
#include <iostream>
#include <string>
#include <time.h>
#include <unistd.h>

const unsigned long MEMORY_SIZE = 16777216;

class Adept_api_t {
public:
  Adept_api_t(VAdept *_dut) {
    dut = _dut;
    main_time = 0L;
#if VM_TRACE
    tfp = NULL;
#endif
  }

#if VM_TRACE
  void init_dump(VerilatedVcdC *_tfp) { tfp = _tfp; }
#endif

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

  virtual inline void load_memory(std::map<size_t, size_t> memory) {
    // Set memory write enable to high
    dut->io_load_we = 1;
    for (auto const &entry : memory) {
      // Load data
      load_data(entry.first, entry.second);

      // Advance simulation
      step();
    }
    dut->io_load_we = 0;
  }

private:
  VAdept *dut;
  vluint64_t main_time;
#if VM_TRACE
  VerilatedVcdC *tfp;
#endif

  virtual inline void load_data(size_t addr, size_t data) {
    // Set memory address to write to
    dut->io_load_addr_w = addr;

    // Load 32 bits at a time and then advance simulation
    dut->io_load_data_in_0 = (data & 0x000000ff);
    dut->io_load_data_in_1 = (data & 0x0000ff00) >> 8;
    dut->io_load_data_in_2 = (data & 0x00ff0000) >> 16;
    dut->io_load_data_in_3 = (data & 0xff000000) >> 24;
  }
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
    std::cerr << "\t <PROG> --max-cycles=<MAX_CYCLES> <VERILATOR_FLAGS>"
              << std::endl;
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

  // Create Random seed
  srand(time(NULL) ^ getpid());

  // Memory in simulation is 16MB. Initialize by dumping garbage.
  for (size_t i = 0; i < MEMORY_SIZE; i++) {
    memory[i] = rand();
  }

  api.load_memory(memory);

  // We're done loading data into the memory. Lower write enable and reset
  // processor.
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
