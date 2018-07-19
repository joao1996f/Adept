package adept.idecode

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig

class JALControlSignals(override val config: AdeptConfig,
                           instruction: UInt, decoder_out: InstructionDecoderOutput)
    extends InstructionControlSignals(config, instruction, decoder_out) {

  op_code := op_codes.JAL

  def generateControlSignals(config: AdeptConfig, instruction: UInt) = {
    val rsd_sel = instruction(11, 7)
    val imm     = instruction(31, 12)

    io.registers.rsd_sel := rsd_sel
    io.registers.we      := true.B

    io.pc.br_offset      := Cat(imm(19), imm(6, 0), imm(7), imm(18, 8), 0.asUInt(1.W)).asSInt
    // The funct3 in JAL is a don't Care
    io.pc.op             := pc_ops.getPcOp(0.U(3.W), op_codes.JAL)

    io.switch_2_imm      := true.B
    io.immediate         := 4.S // Add 4 to PC

    io.alu.op            := alu_ops.add // Perform an Add between the PC and the immediate

    io.sel_operand_a     := 1.U // Select the PC into the ALU
    io.sel_rf_wb         := 0.U // Select the ALU to write to the register file
  }

}
