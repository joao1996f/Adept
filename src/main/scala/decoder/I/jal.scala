package adept.decoder.integer

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig
import adept.decoder.{InstructionControlSignals, InstructionDecoderOutput}

private class JALControlSignals(override val config: AdeptConfig,
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

    io.immediate         := 4.S // Add 4 to PC

    io.alu.op            := alu_ops.add // Perform an Add between the PC and the immediate

    io.sel_operand_a     := core_ctl_signals.sel_oper_A_pc
    io.sel_operand_b     := core_ctl_signals.sel_oper_B_imm
    io.sel_rf_wb         := core_ctl_signals.result_alu
  }

}
