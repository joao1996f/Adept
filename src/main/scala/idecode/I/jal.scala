package adept.idecode

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig

class JALControlSignals(override val config: AdeptConfig,
                           instruction: UInt)
    extends InstructionControlSignals(config, instruction) {

  op_code := op_codes.JAL

  def generateControlSignals(config: AdeptConfig, instruction: UInt) = {
    val rsd_sel = instruction(11, 7)
    val imm     = instruction(31, 12)

    registers.rsd_sel := rsd_sel
    registers.we      := true.B

    pc.br_offset      := Cat(imm(19), imm(6, 0), imm(7), imm(18, 8), 0.asUInt(1.W)).asSInt

    alu.switch_2_imm  := true.B
    alu.imm           := 4.S // Add 4 to PC
    alu.op            := 0.U // Perform an Add between the PC and the immediate
    alu.op_code       := op_codes.JAL

    sel_operand_a     := 1.U // Select the PC into the ALU
    sel_rf_wb         := 0.U // Select the ALU to write to the register file
  }

}
