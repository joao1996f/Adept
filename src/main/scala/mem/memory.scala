package adept.mem

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig

class MemIO(val config: AdeptConfig) extends Bundle {
  // Inputs
  val data_in = Input(SInt(config.XLen.W))
  val addr    = Input(UInt(config.XLen.W))

  override def cloneType: this.type = {
    new MemIO(config).asInstanceOf[this.type];
  }
}

class MemDecodeIO(val config: AdeptConfig) extends Bundle {
  val op = Input(UInt(config.funct.W))
  val we = Input(Bool())
  val en = Input(Bool())

  override def cloneType: this.type = {
    new MemDecodeIO(config).asInstanceOf[this.type];
  }
}

class Memory(config: AdeptConfig) extends Module {
  val io = IO(new Bundle {
                val in     = new MemIO(config)
                val decode = new MemDecodeIO(config)

                val data_out = Output(SInt(config.XLen.W))
                val stall    = Output(Bool())
              })

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

  val read_port = WireInit(Vec(Seq.fill(4)(0.U(8.W))))
  val addr = io.in.addr >> 2
  val trash = RegNext(addr.asSInt)
  val byte_sel_write = WireInit(io.in.addr(1, 0).asUInt)
  val byte_sel_read = RegNext(io.in.addr(1, 0).asUInt)

  // Stall logic
  // Handshake protocol
  val ready = RegInit(false.B)
  val valid = RegInit(false.B)
  val stall = RegInit(false.B)

  when (io.decode.we && io.decode.en) {
    val new_data = WireInit(Vec(Seq.fill(4)(0.U(8.W))))
    // Store Byte (8 bits)
    new_data(byte_sel_write) := io.in.data_in(7, 0)
    // Store Half (16 bits)
    new_data(byte_sel_write + 1.U) := Mux(io.decode.op(0) === true.B || io.decode.op(1) === true.B,
                                                      io.in.data_in(15, 8), 0.U)
    // Store Word (32 bits)
    new_data(byte_sel_write + 2.U) := Mux(io.decode.op(1) === true.B, io.in.data_in(23, 16), 0.U)
    new_data(byte_sel_write + 3.U) := Mux(io.decode.op(1) === true.B, io.in.data_in(31, 24), 0.U)

    // Build write mask
    val mask = Wire(UInt(4.W))
    mask := Mux(io.decode.op === 0.U, 1.U << byte_sel_write,
                Mux(io.decode.op === 1.U, 3.U << byte_sel_write, 15.U << byte_sel_write))

    // Write
    my_mem.io.addr    := addr
    my_mem.io.we      := io.decode.we
    my_mem.io.re      := ~io.decode.we
    my_mem.io.data_in := new_data
    valid             := true.B
    stall             := true.B
  } .otherwise {
    my_mem.io.addr    := addr
    my_mem.io.we      := io.decode.we
    my_mem.io.re      := ~io.decode.we
    my_mem.io.data_in := Vec(0.U, 0.U, 0.U, 0.U)
    read_port         := my_mem.io.data_out
    when (io.decode.en) {
      valid             := true.B
      stall             := true.B
    } .otherwise {
      valid := false.B
      stall := false.B
    }
  }

  // Stall logic
  // Handshake protocol
  ready           := my_mem.io.ready
  my_mem.io.valid := valid

  when (ready && valid && io.decode.en) {
    stall := false.B
    valid := false.B
  }

  io.stall := stall

  io.data_out := MuxLookup(io.decode.op, trash, Array(
                             // Load Byte (8 bits)
                             0.U -> read_port(byte_sel_read).asSInt,
                             // Load Half (16 bits)
                             1.U -> Cat(read_port(byte_sel_read + 1.U), read_port(byte_sel_read)).asSInt,
                             // Load Word (32 bits)
                             2.U -> Cat(read_port(3), read_port(2), read_port(1), read_port(0)).asSInt,
                             // Load Byte Unsigned (8 bits)
                             4.U -> Cat(0.U, read_port(byte_sel_read)).asSInt,
                             // Load Half Unsigned (16 bits)
                             5.U -> Cat(0.U, read_port(byte_sel_read + 1.U), read_port(byte_sel_read)).asSInt
                           ))
}
