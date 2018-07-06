// See LICENSE for license details.
package adept.alu

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig
import adept.registerfile.RegisterFileOut

/*
 *  This is an ALU used in a RISC-V processor. The main idea behind it is to be
 *  able to generate an ALU for any RISC-V ISA.
 */

class AluIO(config: AdeptConfig) extends Bundle {
  val registers      = Flipped(new RegisterFileOut(config))
  val decoder_params = Input(new DecoderAluIO(config))
}

class DecoderAluIO(val config: AdeptConfig) extends Bundle {
  // Immediate, is sign extended
  val imm          = SInt(config.XLen.W)
  // Operation
  val op           = UInt(config.funct.W)
  val op_code      = UInt(config.op_code.W)
  val switch_2_imm = Bool()

  override def cloneType: this.type = {
    new DecoderAluIO(config).asInstanceOf[this.type]
  }
}

class ALU(config: AdeptConfig) extends Module {
  val io = IO(new Bundle {
                // Input
                val in = new AluIO(config)

                // Output
                val result   = Output(SInt(config.XLen.W))
                val cmp_flag = Output(Bool())
              })

  //////////////////////////////////////////////////////////////////////////////
  // Select operands
  //////////////////////////////////////////////////////////////////////////////
  val operand_A = io.in.registers.rs1.asSInt
  val operand_B = Wire(SInt(config.XLen.W))
  val carry_in = Wire(SInt(config.XLen.W))

  // Select Operand B
  // Immediate instructions
  when(io.in.decoder_params.switch_2_imm) {
    operand_B := io.in.decoder_params.imm
    carry_in := 0.S
  } .otherwise {
    // Register instructions
    val sel_oper_B = io.in.registers.rs2.asSInt
    // Small modification to operand B when performing signed addition. Only occurs on SUBs, BEQs and BNEs
    when (io.in.decoder_params.imm(10) === true.B &&
            (io.in.decoder_params.op_code === "b0110011".U || io.in.decoder_params.op_code === "b1100011".U)
            && io.in.decoder_params.op === 0.U) {
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
  val shift_op                    = operand_B(4, 0).asUInt
  val shift_left_logic_result     = operand_A << shift_op
  val shift_right_result_signed   = operand_A >> shift_op
  val shift_right_result_unsigned = operand_A.asUInt >> shift_op
  val operand_A_shift_sel         = io.in.decoder_params.imm(10)
  val shift_right_result          = Mux(operand_A_shift_sel,
                                        shift_right_result_signed,
                                        shift_right_result_unsigned.asSInt)

  // Set Less Than
  val set_less_result          = Cat(0.U, operand_A < operand_B).asSInt
  val set_less_unsigned_result = Cat(0.U, operand_A.asUInt < operand_B.asUInt).asSInt

  //////////////////////////////////////////////////////////////////////////////
  // Output MUX
  //////////////////////////////////////////////////////////////////////////////
  val result = MuxLookup(io.in.decoder_params.op, -1.S, Array(
                           0.U -> add_result,
                           1.U -> shift_left_logic_result,
                           2.U -> set_less_result,
                           3.U -> set_less_unsigned_result,
                           4.U -> xor_result,
                           5.U -> shift_right_result,
                           6.U -> or_result,
                           7.U -> and_result))

  //////////////////////////////////////////////////////////////////////////////
  // Output
  //////////////////////////////////////////////////////////////////////////////
  io.result := result

  // This flag is only valid when evaluating control instructions
  io.cmp_flag := result.asUInt.orR
}

// This is needed to generate the verilog just for this module. When generating
// the verilog this object will only be needed in the top module.
object ALU extends App {
  val config = new AdeptConfig
  chisel3.Driver.execute(args, () => new ALU(config))
}
