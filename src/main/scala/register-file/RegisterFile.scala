package adept.registerfile

import chisel3._
import chisel3.util.log2Ceil

import adept.config.AdeptConfig
import adept.idecode.DecoderRegisterOut

class RegisterFileOut(val config: AdeptConfig) extends Bundle {
  val rs1 = Output(SInt(config.XLen.W))
  val rs2 = Output(SInt(config.XLen.W))

  override def cloneType: this.type = {
    new RegisterFileOut(config).asInstanceOf[this.type];
  }
}

/**
  A 3-port Register File with a configurable data width and number of registers.
  */
class RegisterFile(val config: AdeptConfig) extends Module {
  val io = IO(new Bundle {
    // Inputs
    val rsd_value  = Input(SInt(config.XLen.W))
    val we         = Input(Bool())
    val decoder    = Flipped(new DecoderRegisterOut(config))

    // Outputs
    val registers = new RegisterFileOut(config)
  })

  // Create a vector of registers
  val registers = Reg(Vec(config.XLen, SInt(config.XLen.W)))

  // Perform write operation
  when (io.we && io.decoder.rsd_sel =/= 0.U) {
    registers(io.decoder.rsd_sel) := io.rsd_value
  }

  // Perform read operation
  io.registers.rs1 := registers(io.decoder.rs1_sel)
  io.registers.rs2 := registers(io.decoder.rs2_sel)
}
