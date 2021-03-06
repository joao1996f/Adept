package adept.decoder.integer

import chisel3._

import adept.config.AdeptConfig
import adept.decoder.{InstructionControlSignals, InstructionDecoderOutput}

// TODO: Throw a trap when the immediate doesn't conform to the spec
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

    io.registers.we      := true.B
    io.registers.rsd_sel := rsd_sel
    io.registers.rs1_sel := rs1_sel
    // Shift instructions don't have rs2. In that case rs2 contains the shift
    // amount.
    io.registers.rs2_sel := rs2_sel

    io.switch_2_imm      := true.B
    io.immediate         := imm.asSInt

    io.alu.op            := alu_ops.getALUOp(op, imm(11, 5), op_codes.Immediate)

    io.sel_rf_wb         := 0.U // Selects ALU to write to the Register File
    io.sel_operand_a     := 0.U // Select RS1 to be an input of the ALU
  }

}
