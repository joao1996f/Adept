package adept.alu

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig

class ALU(config: AdeptConfig) extends Module {
  val io = IO(new Bundle {
                // Input
                // Registers
                val rs1 = Input(UInt(config.XLen.W))
                val rs2 = Input(UInt(config.XLen.W))
                val rsd = Input(UInt(config.rs_len.W))

                // Immediate, is sign extended
                val imm = Input(SInt(config.XLen.W))
                // Operation
                val op = Input(UInt(config.funct.W))
                val op_code = Input(UInt(config.op_code.W))

                // Output
                val result = Output(UInt(config.XLen.W))
              })

  // Select operands
  val operand_A = Wire(UInt(config.XLen.W))
  val sel_oper_B = Wire(UInt(config.XLen.W))
  val operand_B = Wire(UInt(config.XLen.W))
  val carry_in = Wire(Bool())

  // Select Operand A
  when (io.imm(22) === true.B && (io.op_code(1, 2) === 3.U || io.op_code(1, 2) === 1.U)) {
    operand_A := rs1.asSInt
  } .otherwise {
    operand_A := rs1
  }

  // Select Operand B
  // Immediate instructions
  when(io.op_code(1, 2) === 1.U) {
    when (io.op(2, 3) === 1.U) {
      // special case shift
      sel_oper_B := io.imm(27, 31)
    } .otherwise {
      // Regular Immediate
      sel_oper_B := io.imm
    }
  } .otherwise {
    // Register instructions
    sel_oper_B := io.rs2
  }

  // Small modification to operand B when performing signed addition
  when (io.imm(22) === true.B && io.op_code(1, 2) === 3.U) {
    operand_B := not(sel_oper_B)
    carry_in := true.B
  } .otherwise {
    operand_B := sel_oper_B
    carry_in := false.B
  }

  // Execution Units
  // Subtraction is derived from add, two's complement
  val add_result = operand_A + operand_B + carry_in
  val xor_result = operand_A ^ operand_B
  val or_result = operand_A | operand_B
  val and_result = operand_A & operand_B
  val shift_left_logic_result = operand_A << operand_B
  val shift_right_result = operand_A >> operand_B


  io.result :=  MuxLookup(io.op, 0.U, Array(
                            0.U -> add_result,
                            1.U -> shift_left_logic_result,
                            2.U -> set_less_result,
                            3.U -> set_less_unsigned_result,
                            4.U -> xor_result,
                            5.U -> shift_right_result,
                            6.U -> or_result,
                            7.U -> and_result))
}
