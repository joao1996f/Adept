package adept.idecode.integer

import chisel3._

import adept.config.AdeptConfig
import adept.idecode.{InstructionControlSignals, InstructionDecoderOutput}

// TODO: Check check if the function code is 000, else throw trap
private class JalRControlSignals(override val config: AdeptConfig,
                              instruction: UInt, decoder_out: InstructionDecoderOutput)
    extends InstructionControlSignals(config, instruction, decoder_out) {

  op_code := op_codes.JALR

  def generateControlSignals(config: AdeptConfig, instruction: UInt) = {
    val rsd_sel = instruction(11, 7)
    val op      = instruction(14, 12)
    val rs1_sel = instruction(19, 15)
    val imm     = instruction(31, 20)

    io.registers.we      := true.B
    io.registers.rsd_sel := rsd_sel
    io.registers.rs1_sel := rs1_sel

    io.switch_2_imm      := true.B
    io.immediate         := 4.S // Add 4 to PC

    io.alu.op            := alu_ops.add // Perform PC + 4

    io.pc.br_offset      := imm.asSInt // Pass immediate to PC
    io.pc.op             := pc_ops.getPcOp(op, op_codes.JALR)

    io.sel_rf_wb         := 0.U // Selects ALU to write to the Register File
    io.sel_operand_a     := 1.U // Select PC to be an input of the ALU
  }

}
