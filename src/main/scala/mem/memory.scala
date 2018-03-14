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

  when (io.in.we) {
    val addr_w = io.in.addr_w >> 2
    val new_data = WireInit(Vec(Seq.fill(4)(0.U(4.W))))
    // Store Byte (8 bits)
    new_data(io.in.addr_w(1, 0).asUInt) := io.in.data_in(7, 0)
    // Store Half (16 bits)
    new_data(io.in.addr_w(1, 0).asUInt + 1.U) := Mux(io.op(0) === true.B || io.op(1) === true.B,
                                                      io.in.data_in(15, 8), 0.U)
    // Store Word (32 bits)
    new_data(io.in.addr_w(1, 0).asUInt + 2.U) := Mux(io.op(1) === true.B, io.in.data_in(23, 16), 0.U)
    new_data(io.in.addr_w(1, 0).asUInt + 3.U) := Mux(io.op(1) === true.B, io.in.data_in(31, 24), 0.U)

    // Build write mask
    val mask = Wire(UInt(4.W))
    mask := Mux(io.op === 0.U, 1.U << io.in.addr_w(1, 0),
                Mux(io.op === 1.U, 3.U << io.in.addr_w(1, 0), 15.U << io.in.addr_w(1, 0)))

    // Write
    my_mem.write(addr_w, new_data, mask.toBools)
    io.data_out := 0.S
  } .otherwise {
    val addr_r = io.in.addr_r >> 2
    val read_result = my_mem.read(addr_r)
    io.data_out := MuxLookup(io.op, 0.S, Array(
                               // Load Byte (8 bits)
                               0.U -> read_result(io.in.addr_r(1, 0)).asSInt,
                               // Load Half (16 bits)
                               1.U -> Cat(read_result(io.in.addr_r(1, 0) + 1.U), read_result(io.in.addr_r(1, 0))).asSInt,
                               // Load Word (32 bits)
                               2.U -> Cat(read_result(3), read_result(2), read_result(1), read_result(0)).asSInt,
                               // Load Byte Unsigned (8 bits)
                               4.U -> Cat(0.U, read_result(io.in.addr_r(1, 0))).asSInt,
                               // Load Half Unsigned (16 bits)
                               5.U -> Cat(0.U, read_result(io.in.addr_r(1, 0) + 1.U), read_result(io.in.addr_r(1, 0))).asSInt
                             ))
  }
}
