package adept.mem

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig

class MemIO(val config: AdeptConfig) extends Bundle {
  // Inputs
  val data_in = Input(SInt(config.XLen.W))
  val addr_w  = Input(UInt(config.XLen.W))
  val addr_r  = Input(UInt(config.XLen.W))
  val we      = Input(Bool())

  override def cloneType: this.type = {
    new MemIO(config).asInstanceOf[this.type];
  }
}

class Memory(config: AdeptConfig) extends Module {
  val io = IO(new Bundle {
                val in = new MemIO(config)
                val op = Input(UInt(config.funct.W))

                val data_out = Output(SInt(config.XLen.W))
              })

  // Because the core is made up of one stage, the memory needs to return a
  // result in the same clock cycle. The memory uses 8MB.
  val my_mem = Mem(Vec(4, UInt(8.W)), 1 << 21)
  val addr_w = io.in.addr_w.asUInt >> 2
  val addr_r = io.in.addr_r.asUInt >> 2

  when (io.in.we) {
    val new_data = Wire(Vec(4, UInt(8.W)))
    // Store Byte (8 bits)
    new_data(0) := io.in.data_in(7, 0)
    // Store Half (16 bits)
    new_data(1) := Mux(io.op(0) === true.B || io.op(1) === true.B,
                       io.in.data_in(15, 8), 0.U),
    // Store Word (32 bits)
    new_data(2) := Mux(io.op(1) === true.B, io.in.data_in(23, 16), 0.U)
    new_data(3) := Mux(io.op(1) === true.B, io.in.data_in(31, 24), 0.U)

    my_mem.write(addr_w(20, 0), new_data)
    io.data_out := 0.U

  } .otherwise {
    val read_result = my_mem.read(addr_r(20, 0))
    io.data_out := MuxLookup(io.op, 0.S, Array(
                               // Load Byte (8 bits)
                               0.U -> read_result(0),
                               // Load Half (16 bits)
                               1.U -> Cat(read_result(1), read_result(0)),
                               // Load Word (32 bits)
                               2.U -> Cat(read_result(3), read_result(2), read_result(1), read_result(0)),
                               // Load Byte Unsigned (8 bits)
                               4.U -> Cat(0.U, read_result(0)),
                               // Load Half Unsigned (16 bits)
                               5.U -> Cat(0.U, read_result(1), read_result(1)),
                             ))
  }
}
