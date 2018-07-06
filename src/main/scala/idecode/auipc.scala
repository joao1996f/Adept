package adept.idecode

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig

class AUIPCControlSignals(override val config: AdeptConfig,
                           instruction: UInt)
    extends InstructionControlSignals(config, instruction) {

  op_code := op_codes.AUIPC

  def generateControlSignals(config: AdeptConfig, instruction: UInt) = {
    val rsd_sel = instruction(11, 7)
    val imm     = instruction(31, 12)

    registers.rsd_sel := rsd_sel
    registers.we      := true.B

    alu.switch_2_imm := true.B
    alu.imm          := Cat(imm, Fill(12, "b0".U)).asSInt
    alu.op           := 0.U // Select an Add

    sel_operand_a    := 1.U // Select PC for operand A of the ALU

    sel_rf_wb        := 0.U // Write result of the ALU to the register file
  }

}
