package adept.idecode

import chisel3._

import adept.config.AdeptConfig

// TODO: Validate function op, else throw trap.
class StoresControlSignals(override val config: AdeptConfig,
                         instruction: UInt)
    extends InstructionControlSignals(config, instruction) {

  op_code := op_codes.Stores

  val op      = instruction(14, 12)
  val rs1_sel = instruction(19, 15)
  val rs2_sel = instruction(24, 20)

  def generateControlSignals(config: AdeptConfig) = {
    registers.rs1_sel := rs1_sel
    registers.rs2_sel := rs2_sel

    alu.switch_2_imm  := true.B
    alu.imm           := Cat(instruction(31, 25), instruction(11, 7)).asSInt
    alu.op            := 0.U // Perform ADD in the ALU between rs1 and the immediate

    sel_operand_a     := 0.U

    mem.we            := true.B
    mem.op            := op
    mem.en            := true.B
  }

}
