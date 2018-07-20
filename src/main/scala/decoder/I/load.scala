package adept.decoder.integer

import chisel3._

import adept.config.AdeptConfig
import adept.decoder.{InstructionControlSignals, InstructionDecoderOutput}

private class LoadControlSignals(override val config: AdeptConfig,
                         instruction: UInt, decoder_out: InstructionDecoderOutput)
    extends InstructionControlSignals(config, instruction, decoder_out) {

  op_code := op_codes.Loads

  def generateControlSignals(config: AdeptConfig, instruction: UInt) = {
    val rsd_sel = instruction(11, 7)
    val op      = instruction(14, 12)
    val rs1_sel = instruction(19, 15)
    val imm     = instruction(31, 20).asSInt

    io.registers.we      := true.B
    io.registers.rsd_sel := rsd_sel
    io.registers.rs1_sel := rs1_sel

    io.switch_2_imm      := true.B
    io.immediate         := imm

    io.alu.op            := alu_ops.add // Add to create relative address

    io.sel_rf_wb         := core_ctl_signals.result_mem
    io.sel_operand_a     := core_ctl_signals.sel_oper_A_rs1

    io.mem.en            := true.B
    io.mem.op            := mem_ops.getMemOp(op, op_codes.Loads)
  }

}
