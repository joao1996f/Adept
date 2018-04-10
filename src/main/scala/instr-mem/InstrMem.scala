package adept.instructionMemory

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig

class InstrMem (config: AdeptConfig) extends Module {
  val io = IO (new Bundle{
    // Inputs
    val in_pc   = Input(UInt(config.XLen.W))
    val data_in = Input(UInt(config.XLen.W))
    val addr_w  = Input(UInt(config.XLen.W))
    val we      = Input(Bool())

    // Outputs
    val instr = Output(UInt(config.XLen.W))
  })

  val mem = SyncReadMem(UInt(config.XLen.W), 1 << 10)
  when (io.we) {
    mem.write(io.addr_w, io.data_in)
    io.instr := 0.U
  } .otherwise {
      val instr = mem(io.in_pc)
      io.instr := instr
  }
}
