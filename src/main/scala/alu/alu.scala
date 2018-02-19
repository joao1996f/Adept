package adept.alu

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig

class ALU(config: AdeptConfig) extends Module {
  val io = IO(new Bundle {
                // Input
                // Registers
                val rs1 = Input(UInt(config.rs_len.W))
                val rs2 = Input(UInt(config.rs_len.W))
                val rsd = Input(UInt(config.rs_len.W))

                // Immediate, is sign extended
                val imm = Input(SInt(config.XLen.W))
                // Operation
                val op = Input(UInt(config.funct.W))
                val op_code = Input(UInt(config.op_code.W))

                // Output
                val result = Output(UInt(config.XLen.W))
              })

  // Immediate instructions
  when(io.op_code(1,2) === "01") {

  }

  // Execution Units
  // Subtraction is derived from add, two's complement
  val add_result = rs1 + operand_B
  val xor_result = rs1 ^ operand_B
  val or_result = rs1 | operand_B
  val and_result = rs1 & operand_B
  val shift_left_logic_result = rs1 << operand_B
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
