package adept.decoder.integer

import chisel3._

import adept.config.AdeptConfig
import adept.decoder.{InstructionControlSignals, InstructionDecoderOutput}

private class RegisterControlSignals(override val config: AdeptConfig,
                         instruction: UInt, decoder_out: InstructionDecoderOutput)
    extends InstructionControlSignals(config, instruction, decoder_out) {

  op_code := op_codes.Registers

  def generateControlSignals(config: AdeptConfig, instruction: UInt) = {
    val rsd_sel = instruction(11, 7)
    val op      = instruction(14, 12)
    val rs1_sel = instruction(19, 15)
    val rs2_sel = instruction(24, 20)
    val imm     = instruction(31, 25)

    io.registers.rs1_sel := rs1_sel
    io.registers.rs2_sel := rs2_sel
    io.registers.rsd_sel := rsd_sel
    io.registers.we      := true.B

    val alu_op            = alu_ops.getALUOp(op, imm, op_codes.Registers)
    io.alu.op            := alu_op

    io.immediate         := imm.asSInt

    io.sel_operand_a     := core_ctl_signals.sel_oper_A_rs1
    io.sel_operand_b     := core_ctl_signals.sel_oper_B_rs2
    io.sel_rf_wb         := core_ctl_signals.result_alu

    // Check if the 7 MSBs respect the instruction set
    when (((alu_op === alu_ops.sub || alu_op === alu_ops.sra) &&
          imm =/= "b0100000".U) || (imm =/= 0.U && alu_op =/= alu_ops.sub &&
          alu_op =/= alu_ops.sra)) {
      io.trap := true.B
    } .otherwise {
      io.trap := false.B
    }
  }

}
