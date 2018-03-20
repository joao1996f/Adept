package adept.instructionMemory

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig

/*
 * ROM memory implemented using a vector.
 * Needs to be filled with instructions.
*/

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
  val count = RegInit(0.U(1.W))

  when (io.we) {
    mem.write(io.addr_w, io.data_in)
    io.instr := 0.U
  } .otherwise {
    when (count === 0.U) {
      val instr = mem(io.in_pc)
      io.instr := instr
      // Detect Branch or Jump instructions and send a Bubble next cycle
      when (instr(6, 0) === "b1100011".U ||
              instr(6, 0) === "b1100111".U ||
              instr(6, 0) === "b1101111".U) {
        count := 1.U
      } .otherwise {
        count := 0.U
      }
    } .otherwise {
      io.instr := 0.U
      count := 0.U
    }
  }
}
