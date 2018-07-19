package adept.decoder.integer

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig
import adept.decoder.{InstructionControlSignals, InstructionDecoderOutput}

// TODO: Validate function op, else throw trap.
private class BranchesControlSignals(override val config: AdeptConfig,
                           instruction: UInt, decoder_out: InstructionDecoderOutput)
    extends InstructionControlSignals(config, instruction, decoder_out) {

  op_code := op_codes.Branches

  def generateControlSignals(config: AdeptConfig, instruction: UInt) = {
    val op      = instruction(14, 12)
    val rs1_sel = instruction(19, 15)
    val rs2_sel = instruction(24, 20)

    io.registers.rs1_sel := rs1_sel
    io.registers.rs2_sel := rs2_sel

    io.pc.br_offset      := Cat(instruction(31), instruction(7),
                             instruction(30, 25), instruction(11, 8),
                             0.asUInt(1.W)).asSInt
    io.pc.op             := pc_ops.getPcOp(op, op_codes.Branches)

    io.switch_2_imm      := false.B

    // Select ALU op depending on branch type
    when (op === "b000".U || op === "b001".U) {
      io.alu.op    := alu_ops.sub // Perform a SUB
    } .elsewhen (op === "b100".U || op === "b101".U) {
      io.alu.op := alu_ops.slt // Perform a set less than
    } .otherwise {
      io.alu.op := alu_ops.sltu // Perform a set less than unsigned
    }

    io.sel_operand_a := 0.U // Select RS1 to be read by the ALU
  }

}
