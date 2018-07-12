package adept.idecode

import chisel3._

import adept.config.AdeptConfig

class LoadControlSignals(override val config: AdeptConfig,
                         instruction: UInt)
    extends InstructionControlSignals(config, instruction) {

  op_code := op_codes.Loads

  def generateControlSignals(config: AdeptConfig, instruction: UInt) = {
    registers.we      := true.B
    registers.rsd_sel := instruction(11, 7)
    registers.rs1_sel := instruction(19, 15)

    alu.switch_2_imm := true.B
    alu.imm          := instruction(31, 20).asSInt
    alu.op           := 0.U // Add to create relative address
    alu.op_code      := op_codes.Loads

    sel_rf_wb     := 1.U // Selects Memory to write to the Register File
    sel_operand_a := 0.U // Select RS1 to be an input of the ALU

    mem.en := true.B
    mem.op := instruction(14, 12)
  }

}
