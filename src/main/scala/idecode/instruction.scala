package adept.idecode

import chisel3._

import adept.config.AdeptConfig

// Import Decoding Interfaces
import adept.mem.DecoderMemIO
import adept.alu.DecoderAluIO
import adept.registerfile.DecoderRegisterFileIO
import adept.pc.DecoderPcIO

import adept.alu.AluOps
import adept.pc.PcOps

final class OpCodes {
  val LUI       = "b0110111".U(7.W)
  val AUIPC     = "b0010111".U(7.W)
  val Branches  = "b1100011".U(7.W)
  val JAL       = "b1101111".U(7.W)
  val JALR      = "b1100111".U(7.W)
  val Loads     = "b0000011".U(7.W)
  val Stores    = "b0100011".U(7.W)
  val Immediate = "b0010011".U(7.W)
  val Registers = "b0110011".U(7.W)
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

  // Enumerate for Pc operations
  val pc_ops = PcOps

  io.registers.setDefaults
  io.alu.setDefaults
  io.pc.setDefaults
  io.mem.setDefaults

  io.sel_rf_wb     := DontCare
  io.sel_operand_a := DontCare
  io.immediate     := DontCare
  io.switch_2_imm  := false.B
  io.trap          := false.B

  generateControlSignals(config, instruction)

  // Define your control signals in this method when implementing this class
  def generateControlSignals(config: AdeptConfig, instruction: UInt)
}
