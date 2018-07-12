// See LICENSE for license details.
package adept.registerfile

import chisel3._
import chisel3.util.log2Ceil

import adept.config.AdeptConfig

class DecoderRegisterFileIO(val config: AdeptConfig) extends Bundle {
  // Registers
  val rs1_sel = UInt(config.rs_len.W)
  val rs2_sel = UInt(config.rs_len.W)
  val rsd_sel = UInt(config.rs_len.W)
  val we      = Bool()

  override def cloneType: this.type = {
    new DecoderRegisterFileIO(config).asInstanceOf[this.type]
  }

  def setDefaults = {
    rs1_sel := DontCare
    rs2_sel := DontCare
    rsd_sel := DontCare
    we      := false.B
  }
}

class RegisterFileOut(val config: AdeptConfig) extends Bundle {
  val rs1 = Output(SInt(config.XLen.W))
  val rs2 = Output(SInt(config.XLen.W))

  override def cloneType: this.type = {
    new RegisterFileOut(config).asInstanceOf[this.type]
  }
}

class RegisterFileIO(val config: AdeptConfig) extends Bundle {
  // Inputs
  val rsd_value  = Input(SInt(config.XLen.W))
  val decoder    = Input(new DecoderRegisterFileIO(config))

  // Used in simulation only to print the registers at the end
  val success = if (config.sim) {
    Some(Input(Bool()))
  } else {
    None
  }

  // Outputs
  val registers = new RegisterFileOut(config)

  override def cloneType: this.type = {
    new RegisterFileIO(config).asInstanceOf[this.type]
  }
}

/**
  A 3-port Register File with a configurable data width and number of registers.
  */
class RegisterFile(val config: AdeptConfig) extends Module {
  val io = IO(new RegisterFileIO(config))

  // Create a vector of registers
  val registers = Mem(config.n_registers, SInt(config.XLen.W))

  // Perform write operation
  when (io.decoder.we && io.decoder.rsd_sel =/= 0.U) {
    registers(io.decoder.rsd_sel) := io.rsd_value
  }

  // Perform read operation
  io.registers.rs1 := registers(io.decoder.rs1_sel)
  io.registers.rs2 := registers(io.decoder.rs2_sel)

  // Debug
  if (config.sim && config.verbose >= 1) {
    when (io.success.getOrElse(false.B)) {
      for (i <- 0 until 32) {
        printf("R%d = 0x%x\n", i.U, registers(i))
      }
    }
  }
}
