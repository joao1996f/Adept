package adept.registerfile

import chisel3._
import chisel3.util.log2Ceil

/**
  A 3-port Register File with a configurable data width and number of registers.
  */
class RegisterFile(val data_w: Int, val n_regs: Int) extends Module {
  val io = IO(new Bundle {
    // Inputs
    val rd_value  = Input(UInt(data_w.W))
    val we        = Input(Bool())
    val rd_sel    = Input(UInt(log2Ceil(n_regs).W))
    val rs1_sel   = Input(UInt(log2Ceil(n_regs).W))
    val rs2_sel   = Input(UInt(log2Ceil(n_regs).W))

    // Outputs
    val rs1       = Output(UInt(data_w.W))
    val rs2       = Output(UInt(data_w.W))
  })

  // Create a vector of registers
  val registers = Reg(Vec(n_regs, UInt(data_w.W)))

  // Perform write operation
  when (io.we) {
    registers(io.rd_sel) := io.rd_value
  }

  // Perform read operation
  io.rs1 := registers(io.rs1_sel)
  io.rs2 := registers(io.rs2_sel)
}
