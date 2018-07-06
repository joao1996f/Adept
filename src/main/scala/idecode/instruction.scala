package adept.idecode

import chisel3._

import adept.config.AdeptConfig

// Import Decoding Interfaces
import adept.mem.DecoderMemIO
import adept.alu.DecoderAluIO
import adept.registerfile.DecoderRegisterFileIO
import adept.pc.DecoderPcIO

final class OpCodes {
  val LUI       = "b0110111".U
  val AUIPC     = "b0010111".U
  val Branches  = "b1100011".U
  val JAL       = "b1101111".U
  val JALR      = "b1100111".U
  val Loads     = "b0000011".U
  val Stores    = "b0100011".U
  val Immediate = "b0010011".U
  val Registers = "b0110011".U
}

abstract class Instruction(val config: AdeptConfig) {
  val op_code = Wire(UInt(7.W))

  // Register File Control Signals
  val register = new DecoderRegisterFileIO(config)
  register.setAllDontCare

  // ALU Control Signals
  val alu = new DecoderAluIO(config)
  alu.setAllDontCare

  // PC
  val pc  = new DecoderPcIO(config)
  pc.setAllDontCare

  // Memory Control Signals
  val mem = new DecoderMemIO(config)
  mem.setAllDontCare

  // Select the memory or the ALU to write to the Register File
  val sel_rf_wb = Wire(UInt(1.W))
  sel_rf_wb := DontCare

  // Select Operand A for ALU
  val sel_operand_a = Wire(UInt(1.W))
  sel_operand_a := DontCare
}
