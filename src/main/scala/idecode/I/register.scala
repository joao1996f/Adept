package adept.idecode.integer

import chisel3._

import adept.config.AdeptConfig
import adept.idecode.{InstructionControlSignals, InstructionDecoderOutput}

// TODO: Check if immediate is zero or has a single bit set to one in position
// 5, else throw trap.
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

    io.alu.op            := alu_ops.getALUOp(op, imm, op_codes.Registers)

    io.immediate         := imm.asSInt
    io.switch_2_imm      := false.B

    // Select RS1 and write the ALU result to the register file
    io.sel_operand_a     := 0.U
    io.sel_rf_wb         := 0.U
  }

}
