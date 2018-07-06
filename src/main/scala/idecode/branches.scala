package adept.idecode

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig

// TODO: Validate function op, else throw trap.
class BranchesControlSignals(override val config: AdeptConfig,
                           instruction: UInt)
    extends InstructionControlSignals(config, instruction) {

  op_code := op_codes.Branches

  def generateControlSignals(config: AdeptConfig, instruction: UInt) = {
    val op      = instruction(14, 12)
    val rs1_sel = instruction(19, 15)
    val rs2_sel = instruction(24, 20)

    registers.rs1_sel := rs1_sel
    registers.rs2_sel := rs2_sel

    pc.br_offset      := Cat(instruction(31), instruction(7),
                             instruction(30, 25), instruction(11, 8),
                             0.asUInt(1.W)).asSInt
    alu.switch_2_imm  := false.B

    // Select ALU op depending on branch type
    when (op === "b000".U || op === "b001".U) {
      alu.imm := 1024.S   // Force a subtraction
      alu.op  := "b000".U // Perform a SUB
    } .elsewhen (op === "b100".U || op === "b101".U) {
      alu.op := "b010".U // Perform a set less than
    } .otherwise {
      alu.op := "b011".U // Perform a set less than unsigned
    }

    sel_operand_a := 0.U // Select RS1 to be read by the ALU
  }

}
