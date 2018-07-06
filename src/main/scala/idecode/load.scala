package adept.idecode

import chisel3._

import adept.config.AdeptConfig

class LoadControlSignals(override val config: AdeptConfig,
                         instruction: UInt)
    extends InstructionControlSignals(config, instruction) {

  op_code := op_codes.Loads

  val op      = instruction(14, 12)
  val rsd_sel = instruction(11, 7)
  val rs1_sel = instruction(19, 15)
  val imm     = instruction(31, 20)

  def generateControlSignals(config: AdeptConfig) = {
    registers.we      := true.B
    registers.rsd_sel := rsd_sel
    registers.rs1_sel := rs1_sel

    alu.switch_2_imm := true.B
    alu.imm          := imm.asSInt
    alu.op           := 0.U // Add to create relative address

    sel_rf_wb     := 1.U // Selects Memory to write to the Register File
    sel_operand_a := 0.U // Select RS1 to be an input of the ALU

    mem.en := true.B
    mem.op := op
  }

}
