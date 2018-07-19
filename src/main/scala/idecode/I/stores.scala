package adept.idecode.integer

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig
import adept.idecode.{InstructionControlSignals, InstructionDecoderOutput}

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

    io.switch_2_imm      := true.B
    io.immediate         := Cat(instruction(31, 25), instruction(11, 7)).asSInt

    io.alu.op            := alu_ops.add // Perform ADD in the ALU between rs1 and the immediate

    io.sel_operand_a     := 0.U

    io.mem.we            := true.B
    io.mem.op            := op
    io.mem.en            := true.B
  }

}
