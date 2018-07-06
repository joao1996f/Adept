// See LICENSE for license details.
package adept.mem

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig

class MemIO(val config: AdeptConfig) extends Bundle {
  // Inputs
  val data_in = Input(SInt(config.XLen.W))
  val addr    = Input(UInt(config.XLen.W))

  override def cloneType: this.type = {
    new MemIO(config).asInstanceOf[this.type]
  }
}

class MemLoadIO(config: AdeptConfig) extends Bundle {
  // Inputs
  val data_in = Input(Vec(4, UInt(8.W)))
  val addr_w  = Input(UInt(config.XLen.W))
  val we      = Input(Bool())

  override def cloneType: this.type = {
    new MemLoadIO(config).asInstanceOf[this.type]
  }
}

class DecoderMemIO(val config: AdeptConfig) extends Bundle {
  val op = UInt(config.funct.W)
  val we = Bool()
  val en = Bool()

  override def cloneType: this.type = {
    new DecoderMemIO(config).asInstanceOf[this.type]
  }
}

class Memory(config: AdeptConfig) extends Module {
  val io = IO(new Bundle {
                // Program Load
                val load     = new MemLoadIO(config)

                // Data R/W Ports
                val in       = new MemIO(config)
                val decode   = Input(new DecoderMemIO(config))
                val data_out = Output(SInt(config.XLen.W))

                // Instruction Read Port
                val pc_in     = Input(UInt(config.XLen.W))
                val instr_out = Output(UInt(config.XLen.W))

                val stall     = Output(Bool())
              })

  private def buildWriteData(data_in: UInt, op: UInt, byte_sel_write: UInt) : Vec[UInt] = {
    val new_data = WireInit(Vec(Seq.fill(config.XLen/8)(0.U((config.XLen/4).W))))
    // Store Byte (8 bits)
    new_data(byte_sel_write) := data_in(7, 0)
    // Store Half (16 bits)
    new_data(byte_sel_write + 1.U) := Mux(op(0) === true.B || op(1) === true.B,
                                          data_in(15, 8), 0.U)
    // Store Word (32 bits)
    new_data(byte_sel_write + 2.U) := Mux(op(1) === true.B, data_in(23, 16), 0.U)
    new_data(byte_sel_write + 3.U) := Mux(op(1) === true.B, data_in(31, 24), 0.U)

    return new_data
  }

  private def buildWriteMask(op: UInt, byte_sel_write: UInt) : UInt = {
    val mask = Wire(UInt(4.W))
    mask := Mux(op === 0.U, 1.U << byte_sel_write,
                Mux(op === 1.U, 3.U << byte_sel_write, 15.U << byte_sel_write))

    return mask
  }

  private def buildReadData(op: UInt, byte_sel_read: UInt, read_port: Vec[UInt]) : UInt = {
    val byte_sel_read_reg = RegNext(byte_sel_read)

    return MuxLookup(op, op, Array(
                // Load Byte (8 bits)
                0.U -> Cat(Fill(config.XLen - 8, read_port(byte_sel_read_reg)(7)), read_port(byte_sel_read_reg)),
                // Load Half (16 bits)
                1.U -> Cat(Fill(config.XLen - 16, read_port(byte_sel_read_reg + 1.U)(7)), read_port(byte_sel_read_reg + 1.U), read_port(byte_sel_read_reg)),
                // Load Word (32 bits)
                2.U -> Cat(read_port(3), read_port(2), read_port(1), read_port(0)),
                // Load Byte Unsigned (8 bits)
                4.U -> Cat(0.U, read_port(byte_sel_read_reg)),
                // Load Half Unsigned (16 bits)
                5.U -> Cat(0.U, read_port(byte_sel_read_reg + 1.U), read_port(byte_sel_read_reg))
              ))
  }

  // val my_mem = if (config.sim_mem) {
    // The memory in simulation uses 131kB. You can also turn this option on
    // when operating on an FPGA. Know that this will instantiate BRAMs, and
    // these will only be available to the core. Feel free to increase the size
    // of the memory.
    val my_mem = Module(new CacheSim(config))
  // } else {
    // Create interface for ASIC external memory
    // Module(new Cache(config))
  // }

  val read_port = WireInit(Vec(Seq.fill(config.XLen/8)(0.U((config.XLen/4).W))))
  val addr = io.in.addr >> 2

  // Pass PC to memory
  my_mem.io.cache.pc_in := io.pc_in

  // Connect Program Loading interface
  my_mem.io.load <> io.load

  // Pass address, read and write enable
  my_mem.io.cache.addr := addr
  my_mem.io.cache.we   := io.decode.we

  when (io.decode.we && io.decode.en) {
    // Write Port
    my_mem.io.cache.data_in := buildWriteData(io.in.data_in.asUInt, io.decode.op, io.in.addr(1, 0).asUInt)
    my_mem.io.cache.mask    := buildWriteMask(io.decode.op, io.in.addr(1, 0).asUInt).toBools
  } .otherwise {
    // Read Port
    my_mem.io.cache.data_in := Vec(0.U, 0.U, 0.U, 0.U)
    my_mem.io.cache.mask    := Vec(false.B, false.B, false.B, false.B)
    read_port               := my_mem.io.cache.data_out
  }

  // Stall logic
  // Handshake protocol with Cache
  val ready = RegInit(false.B)
  val valid = RegInit(false.B)
  val stall = RegInit(false.B)

  ready                 := my_mem.io.cache.ready
  my_mem.io.cache.valid := valid

  when (io.decode.en) {
    valid := true.B
    stall := true.B
  } .otherwise {
    valid := false.B
    stall := false.B
  }

  when (ready && valid && io.decode.en) {
    stall := false.B
    valid := false.B
  }

  ////////////////////////////////////////////////////////////////////////////////
  // Outputs
  ////////////////////////////////////////////////////////////////////////////////
  io.stall := stall

  // Data Read Port
  io.data_out := buildReadData(io.decode.op, io.in.addr(1, 0).asUInt, read_port).asSInt

  // Instruction Read Port
  io.instr_out := my_mem.io.cache.pc_out
}
