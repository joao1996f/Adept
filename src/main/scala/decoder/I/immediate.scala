package adept.decoder.integer

import chisel3._

import adept.config.AdeptConfig
import adept.decoder.{InstructionControlSignals, InstructionDecoderOutput}

private class ImmediateControlSignals(override val config: AdeptConfig,
                              instruction: UInt, decoder_out: InstructionDecoderOutput)
    extends InstructionControlSignals(config, instruction, decoder_out) {

  op_code := op_codes.Immediate

  def generateControlSignals(config: AdeptConfig, instruction: UInt) = {
    val rsd_sel = instruction(11, 7)
    val op      = instruction(14, 12)
    val rs1_sel = instruction(19, 15)
    val rs2_sel = instruction(24, 20)
    val imm     = instruction(31, 20)
    val msb_imm = imm(11, 5)

    io.registers.we      := true.B
    io.registers.rsd_sel := rsd_sel
    io.registers.rs1_sel := rs1_sel
    // Shift instructions don't have rs2. In that case rs2 contains the shift
    // amount.
    io.registers.rs2_sel := rs2_sel

    io.immediate         := imm.asSInt
        
    val alu_op            = alu_ops.getALUOp(op, imm(11, 5), op_codes.Immediate)
    io.alu.op            := alu_op

    io.sel_rf_wb         := core_ctl_signals.result_alu
    io.sel_operand_a     := core_ctl_signals.sel_oper_A_rs1
    io.sel_operand_b     := core_ctl_signals.sel_oper_B_imm

    // Check if the 7 MSBs conform to the spec when the operation is a shift
    // immediate
    when ((msb_imm =/= 0.U && alu_op === alu_ops.sll) ||
	 ((msb_imm =/= "b0100000".U && msb_imm =/= 0.U)
	 && alu_op === alu_ops.srl)){
      io.trap := true.B
    } .otherwise {
      io.trap := false.B
    }
  }

}
