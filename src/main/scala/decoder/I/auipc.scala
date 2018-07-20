package adept.decoder.integer

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig
import adept.decoder.{InstructionControlSignals, InstructionDecoderOutput}

private class AUIPCControlSignals(override val config: AdeptConfig,
                           instruction: UInt, decoder_out: InstructionDecoderOutput)
    extends InstructionControlSignals(config, instruction, decoder_out) {

  op_code := op_codes.AUIPC

  def generateControlSignals(config: AdeptConfig, instruction: UInt) = {
    val rsd_sel = instruction(11, 7)
    val imm     = instruction(31, 12)

    io.registers.rsd_sel := rsd_sel
    io.registers.we      := true.B

    io.switch_2_imm    := true.B
    io.immediate       := Cat(imm, Fill(12, "b0".U)).asSInt

    io.alu.op          := alu_ops.add

    io.sel_operand_a   := core_ctl_signals.sel_oper_A_pc
    io.sel_rf_wb       := core_ctl_signals.result_alu
  }

}
