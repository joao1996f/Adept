package adept.idecode

import chisel3._

import adept.config.AdeptConfig

// Import Decoding Interfaces
import adept.mem.DecoderMemIO
import adept.alu.DecoderAluIO
import adept.registerfile.DecoderRegisterFileIO
import adept.pc.DecoderPcIO

import adept.alu.AluOps

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

abstract class InstructionControlSignals(val config: AdeptConfig,
                                         instruction: UInt,
                                         decoder_out: InstructionDecoderOutput) {
  // Generate List of possible Op Codes
  val op_codes = new OpCodes

  val op_code = Wire(UInt(7.W))
  op_code := DontCare

  // Outputs of the decoder
  val io = Wire(decoder_out)

  // Enumerate for ALU operations
  val alu_ops = AluOps

  io.registers.setDefaults
  io.alu.setDefaults
  io.pc.setDefaults
  io.mem.setDefaults
  io.sel_rf_wb := DontCare
  io.sel_operand_a := DontCare
  io.trap := false.B

  generateControlSignals(config, instruction)

  // Define your control signals in this method when implementing this class
  def generateControlSignals(config: AdeptConfig, instruction: UInt)
}
