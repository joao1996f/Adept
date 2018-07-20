package adept.decoder.integer

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig
import adept.decoder.{InstructionControlSignals, InstructionDecoderOutput}

// TODO: Validate function op, else throw trap.
private class StoresControlSignals(override val config: AdeptConfig,
                         instruction: UInt, decoder_out: InstructionDecoderOutput)
    extends InstructionControlSignals(config, instruction, decoder_out) {

  op_code := op_codes.Stores

  def generateControlSignals(config: AdeptConfig, instruction: UInt) = {
    val op      = instruction(14, 12)
    val rs1_sel = instruction(19, 15)
    val rs2_sel = instruction(24, 20)

    io.registers.rs1_sel := rs1_sel
    io.registers.rs2_sel := rs2_sel

    io.immediate         := Cat(instruction(31, 25), instruction(11, 7)).asSInt

    io.alu.op            := alu_ops.add // Perform ADD in the ALU between rs1 and the immediate

    io.sel_operand_a     := core_ctl_signals.sel_oper_A_rs1
    io.sel_operand_b     := core_ctl_signals.sel_oper_B_imm

    io.mem.we            := true.B
    io.mem.op            := mem_ops.getMemOp(op, op_codes.Stores)
    io.mem.en            := true.B
  }

}
