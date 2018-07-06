package adept.idecode

import chisel3._

import adept.config.AdeptConfig

// TODO: Throw a trap when the immediate doesn't conform to the spec
class ImmediateControlSignals(override val config: AdeptConfig,
                              instruction: UInt)
    extends InstructionControlSignals(config, instruction) {

  op_code := op_codes.Immediate

  val op      = instruction(14, 12)
  val rsd_sel = instruction(11, 7)
  val rs1_sel = instruction(19, 15)
  val rs2_sel = instruction(24, 20)
  val imm     = instruction(31, 20)

  def generateControlSignals(config: AdeptConfig) = {
    registers.we      := true.B
    registers.rsd_sel := rsd_sel
    registers.rs1_sel := rs1_sel
    // Shift instructions don't have rs2. In that case rs2 contains the shift
    // amount.
    registers.rs2_sel := rs2_sel

    alu.switch_2_imm := true.B
    alu.imm          := imm.asSInt
    alu.op           := op

    sel_rf_wb     := 0.U // Selects ALU to write to the Register File
    sel_operand_a := 0.U // Select RS1 to be an input of the ALU
  }

}
