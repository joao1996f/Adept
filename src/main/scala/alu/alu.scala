package adept.alu

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig

/*
 *  This is an ALU used in a RISC-V processor. The main idea behind it is to be
 *  able to generate an ALU for any RISC-V ISA. Currently, it only supports the
 *  base instruction set for the R-Type and I-Type instructions.
 *
 *  TODO:
 *  - S-Type
 *  - B-Type
 *  - U-Type
 *  - J-Type
 *  - Add SLT and SLTU instructions
 */
class ALU(config: AdeptConfig) extends Module {
  val io = IO(new Bundle {
                // Input
                // Registers
                val rs1 = Input(SInt(config.XLen.W))
                val rs2 = Input(SInt(config.XLen.W))
                val rsd = Input(UInt(config.rs_len.W))

                // Immediate, is sign extended
                val imm = Input(SInt(config.XLen.W))
                // Operation
                val op = Input(UInt(config.funct.W))
                val op_code = Input(UInt(config.op_code.W))

                // Output
                val result = Output(SInt(config.XLen.W))
              })

  //////////////////////////////////////////////////////////////////////////////
  // Select operands
  //////////////////////////////////////////////////////////////////////////////
  val operand_A = io.rs1
  val operand_B = Wire(SInt(config.XLen.W))
  val carry_in = Wire(SInt(config.XLen.W))
  val operand_A_shift_sel = Wire(Bool())

  // Select Operand A for right shift
  operand_A_shift_sel := io.imm(10)

  // Select Operand B
  // Immediate instructions
  when(io.op_code(5, 4) === "b01".U) {
    operand_B := io.imm
    carry_in := 0.S
  } .otherwise {
    // Register instructions
    val sel_oper_B = io.rs2
    // Small modification to operand B when performing signed addition
    when (io.imm(10) === true.B && io.op_code(5, 4) === "b11".U && io.op === "b000".U && io.op_code(2) === false.B) {
      operand_B := (~(sel_oper_B.asUInt)).asSInt
      // Issue #122 firrt-interpreter
      // operand_B := ~sel_oper_B
      carry_in := 1.S
    } .otherwise {
      operand_B := sel_oper_B
      carry_in := 0.S
    }
  }

  //////////////////////////////////////////////////////////////////////////////
  // Execution Units
  //////////////////////////////////////////////////////////////////////////////

  // Subtraction is derived from add, two's complement
  val add_result                  = operand_A + operand_B + carry_in
  val xor_result                  = operand_A ^ operand_B
  val or_result                   = operand_A | operand_B
  val and_result                  = operand_A & operand_B

  // Shifts
  val shift_left_logic_result     = operand_A << operand_B(4, 0).asUInt
  val shift_right_result_signed   = operand_A >> operand_B(4, 0)
  val shift_right_result_unsigned = operand_A.asUInt >> operand_B(4, 0)
  val shift_right_result          = Mux(operand_A_shift_sel,
                                        shift_right_result_signed,
                                        shift_right_result_unsigned.asSInt)

  // Set Less Than
  val set_less_result          = Cat(0.U, operand_A < operand_B).asSInt
  val set_less_unsigned_result = Cat(0.U, operand_A.asUInt < operand_B.asUInt).asSInt

  //////////////////////////////////////////////////////////////////////////////
  // Output MUX
  //////////////////////////////////////////////////////////////////////////////
  io.result := MuxLookup(io.op, -1.S, Array(
                         0.U -> add_result,
                         1.U -> shift_left_logic_result,
                         2.U -> set_less_result,
                         3.U -> set_less_unsigned_result,
                         4.U -> xor_result,
                         5.U -> shift_right_result,
                         6.U -> or_result,
                         7.U -> and_result))
}

// This is needed to generate the verilog just for this module. When generating
// the verilog this object will only be needed in the top module.
object ALU extends App {
  val config = new AdeptConfig
  chisel3.Driver.execute(args, () => new ALU(config))
}
