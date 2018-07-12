// See LICENSE for license details.
package adept.idecode

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig
import adept.mem.DecoderMemIO
import adept.alu.DecoderAluIO
import adept.registerfile.DecoderRegisterFileIO
import adept.pc.DecoderPcIO

class InstructionDecoder(config: AdeptConfig) extends Module {
  val io = IO(new Bundle{
                // Input
                val instruction = Input(UInt(config.XLen.W))
                val stall_reg   = Input(Bool())

                // Output
                val registers = Output(new DecoderRegisterFileIO(config))
                val alu       = Output(new DecoderAluIO(config))
                val mem       = Output(new DecoderMemIO(config))
                val pc        = Output(new DecoderPcIO(config))

                // ALU selection control signals
                val sel_operand_a = Output(UInt(1.W))
                // Write Back selection signals
                val sel_rf_wb     = Output(UInt(1.W))
              })

  val instruction = io.instruction

  // Ignore current instruction when the previous was a control instruction
  op_code        := Mux(io.stall_reg, 0.U, instruction(6, 0))

  //////////////////////////////////////////////////////
  // I-Type Decode
  //////////////////////////////////////////////////////
  val load_decode = new LoadControlSignals(config, instruction)
  val jalr_decode = new JalRControlSignals(config, instruction)
  val imm_decode  = new ImmediateControlSignals(config, instruction)

  //////////////////////////////////////////////////////
  // R-Type Decode
  //////////////////////////////////////////////////////
  val registers_decode = new RegisterControlSignals(config, instruction)

  //////////////////////////////////////////////////////
  // S-Type Decode
  //////////////////////////////////////////////////////
  val stores_decode = new StoresControlSignals(config, instruction)

  //////////////////////////////////////////////////////
  // B-Type Decode
  //////////////////////////////////////////////////////
  val branches_decode = new BranchesControlSignals(config, instruction)

  //////////////////////////////////////////////////////
  // U-Type Decode
  //////////////////////////////////////////////////////
  val lui_decode = new LUIControlSignals(config, instruction)
  val auipc_decode = new AUIPCControlSignals(config, instruction)

  //////////////////////////////////////////////////////
  // J-Type Decode
  //////////////////////////////////////////////////////
  val jal_decode = new JALControlSignals(config, instruction)

  //////////////////////////////////////////////////////
  // Invalid Instruction executes a NOP, and sets trap to 1
  //////////////////////////////////////////////////////
  // .otherwise {
  //   io.registers.we      := false.B
  //   io.mem.we            := false.B
  //   mem_en               := false.B
  // }
}
