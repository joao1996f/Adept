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

abstract class InstructionControlSignals(val config: AdeptConfig, instruction: UInt) {
  // Generate List of possible Op Codes
  val op_codes = new OpCodes

  val op_code = Wire(UInt(7.W))
  op_code := DontCare

  // Register File Control Signals
  val registers = new DecoderRegisterFileIO(config)
  registers.setDefaults

  // ALU Control Signals
  val alu = new DecoderAluIO(config)
  alu.setDefaults

  // PC
  val pc  = new DecoderPcIO(config)
  pc.setDefaults

  // Memory Control Signals
  val mem = new DecoderMemIO(config)
  mem.setDefaults

  // Select the memory or the ALU to write to the Register File
  val sel_rf_wb = Wire(UInt(1.W))
  sel_rf_wb := DontCare

  // Select Operand A for ALU
  val sel_operand_a = Wire(UInt(1.W))
  sel_operand_a := DontCare

  // Execute control signals when instruction op code is equal to the op code of the implemented
  // instruction type
  when (instruction(6, 0) === op_code) {
    generateControlSignals(config)
  }

  // Define your control signals in this method when implementing this class
  def generateControlSignals(config: AdeptConfig)
}
