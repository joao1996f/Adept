// See LICENSE for license details.
package adept.decoder

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig
import adept.mem.DecoderMemIO
import adept.alu.DecoderAluIO
import adept.registerfile.DecoderRegisterFileIO
import adept.pc.DecoderPcIO

import adept.decoder.integer.IntegerDecoder

import scala.collection.mutable.ArrayBuffer

class InstructionDecoderOutput(config: AdeptConfig) extends Bundle {
  val registers = new DecoderRegisterFileIO(config)
  val alu       = new DecoderAluIO(config)
  val mem       = new DecoderMemIO(config)
  val pc        = new DecoderPcIO(config)

  // ALU selection control signals
  val sel_operand_a = UInt(2.W)
  val sel_operand_b = UInt(2.W)
  // Write Back selection signals
  val sel_rf_wb     = UInt(1.W)
  // Select immediate in operand B of the ALU
  val immediate     = SInt(config.XLen.W)

  // Trap
  val trap          = Bool()

  override def cloneType: this.type = {
    new InstructionDecoderOutput(config).asInstanceOf[this.type]
  }

  def setDefaults = {
    sel_rf_wb     := DontCare
    sel_operand_a := DontCare
    sel_operand_b := DontCare

    registers.setDefaults
    alu.setDefaults
    pc.setDefaults
    mem.setDefaults

    immediate     := DontCare
    trap          := false.B
  }
}

class InstructionDecoderIO(config: AdeptConfig) extends Bundle {
  val instruction = Input(UInt(config.XLen.W))
  val out         = Output(new InstructionDecoderOutput(config))

  override def cloneType: this.type = {
    new InstructionDecoderIO(config).asInstanceOf[this.type]
  }
}

//////////////////////////////////////////////////////
// Invalid Instruction executes an architecture
// specific NOP, and sets trap to 1
//////////////////////////////////////////////////////
final class InvalidInstruction(decoder_out: InstructionDecoderOutput) {
  // Outputs of the decoder
  val io = Wire(decoder_out)

  io.setDefaults
}

class InstructionDecoder(config: AdeptConfig) extends Module {
  val io = IO(new Bundle{
                val stall_reg = Input(Bool())
                val basic     = new InstructionDecoderIO(config)
              })

  // Ignore current instruction when the previous was a control instruction
  val instruction = Mux(io.stall_reg,
                        "h_0000_0013".U, // Send NOP
                        io.basic.instruction)
  val op_code = instruction(6, 0)

  val decoder_array = ArrayBuffer[(UInt, Bundle)]()

  val invalid         = new InvalidInstruction(new InstructionDecoderOutput(config))
  val integer_decoder = new IntegerDecoder(config, instruction)
  decoder_array ++= integer_decoder.decoder

  io.basic.out := MuxLookup(op_code, invalid.io, decoder_array)
}
