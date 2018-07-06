// See LICENSE for license details.
package adept.registerfile

import chisel3._
import chisel3.util.log2Ceil

import adept.config.AdeptConfig
import adept.idecode.DecoderRegisterOut

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
  val decoder    = Flipped(new DecoderRegisterOut(config))

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
  val registers = Mem(SInt(config.XLen.W), config.n_registers)

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
