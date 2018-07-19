// See LICENSE for license details.
package adept.alu

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig
import adept.registerfile.RegisterFileOut
import adept.decoder.OpCodes

class AluIO(config: AdeptConfig) extends Bundle {
  val decoder_params = Input(new DecoderAluIO(config))
  val operand_A      = Input(SInt(config.XLen.W))
  val operand_B      = Input(SInt(config.XLen.W))
}

final object AluOps {
  val add :: sub :: sll :: slt :: sltu :: xor :: srl :: sra :: or :: and :: Nil = Enum(10)

  private val op_codes = new OpCodes

  def getALUOp(funct: UInt, funct7: UInt, op_code: UInt) : UInt = {
    require(funct.getWidth == 3, "ALU Operations are only 3 bits wide")
    require(op_code.getWidth == 7, "OP Codes are only 7 bits wide")
    require(funct7.getWidth == 7, "Function 7 are only 7 bits wide")

    val result = WireInit(0.U(4.W))

      when (funct === 0.U) {
        when (op_code === op_codes.Immediate || (op_code === op_codes.Registers && funct7 === 0.U)) {
          result := add
        } .otherwise {
          result := sub
        }
      } .elsewhen (funct === 1.U) {
        result := sll
      } .elsewhen (funct === 2.U) {
        result := slt
      } .elsewhen (funct === 3.U) {
        result := sltu
      } .elsewhen (funct === 4.U) {
        result := xor
      } .elsewhen (funct === 5.U) {
        when (funct7 === 0.U) {
          result := srl
        } .otherwise {
          result := sra
        }
      } .elsewhen (funct === 6.U) {
        result := or
      } .otherwise {
        result := and
      }

    return result
  }
}

class DecoderAluIO(val config: AdeptConfig) extends Bundle {
  // log2Ceil of the enum in AluOps
  val op = UInt(4.W)

  override def cloneType: this.type = {
    new DecoderAluIO(config).asInstanceOf[this.type]
  }

  def setDefaults = {
    op := DontCare
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

  val operand_A = io.in.operand_A
  val operand_B = io.in.operand_B

  //////////////////////////////////////////////////////////////////////////////
  // Execution Units
  //////////////////////////////////////////////////////////////////////////////
  val add_result               = operand_A + operand_B
  val sub_result               = operand_A - operand_B
  val xor_result               = operand_A ^ operand_B
  val or_result                = operand_A | operand_B
  val and_result               = operand_A & operand_B

  // Shifts
  val shift_op                 = operand_B(4, 0).asUInt
  val shift_left_logic_result  = operand_A << shift_op
  val shift_right_arith_result = operand_A >> shift_op
  val shift_right_logic_result = (operand_A.asUInt >> shift_op).asSInt

  // Set Less Than
  val set_less_result          = Cat(0.U, operand_A < operand_B).asSInt
  val set_less_unsigned_result = Cat(0.U, operand_A.asUInt < operand_B.asUInt).asSInt

  //////////////////////////////////////////////////////////////////////////////
  // Output MUX
  //////////////////////////////////////////////////////////////////////////////
  val alu_ops = AluOps
  val result = MuxLookup(io.in.decoder_params.op, -1.S, Array(
                           alu_ops.add  -> add_result,
                           alu_ops.sub  -> sub_result,
                           alu_ops.sll  -> shift_left_logic_result,
                           alu_ops.slt  -> set_less_result,
                           alu_ops.sltu -> set_less_unsigned_result,
                           alu_ops.xor  -> xor_result,
                           alu_ops.srl  -> shift_right_logic_result,
                           alu_ops.sra  -> shift_right_arith_result,
                           alu_ops.or   -> or_result,
                           alu_ops.and  -> and_result))

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
