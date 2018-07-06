package adept.idecode

import chisel3._

import adept.config.AdeptConfig

class JalRControlSignals(override val config: AdeptConfig,
                              instruction: UInt)
    extends InstructionControlSignals(config, instruction) {

  op_code := op_codes.JALR

  val op      = instruction(14, 12)
  val rsd_sel = instruction(11, 7)
  val rs1_sel = instruction(19, 15)
  val imm     = instruction(31, 20)

  def generateControlSignals(config: AdeptConfig) = {
    registers.we      := true.B
    registers.rsd_sel := rsd_sel
    registers.rs1_sel := rs1_sel

    alu.switch_2_imm := true.B
    alu.imm          := 4.S // Add 4 to PC
    alu.op           := 0.U // Perform PC + 4

    pc.br_offset  := imm.asSInt // Pass immediate to PC

    sel_rf_wb     := 0.U // Selects ALU to write to the Register File
    sel_operand_a := 1.U // Select PC to be an input of the ALU
  }

}