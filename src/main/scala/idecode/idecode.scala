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

                // Trap
                val trap          = Output(Bool())
              })

  // Ignore current instruction when the previous was a control instruction
  val instruction = Mux(io.stall_reg, 0.U, io.instruction)
  val op_code = instruction(6, 0)

  def connectDecoders(decoder: InstructionControlSignals) = {
    io.registers     := decoder.registers
    io.alu           := decoder.alu
    io.mem           := decoder.mem
    io.pc            := decoder.pc
    io.sel_operand_a := decoder.sel_operand_a
    io.sel_rf_wb     := decoder.sel_rf_wb
    io.trap          := decoder.trap
  }

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

  // Build Decoder
  when (load_decode.op_code === op_code) {
    connectDecoders(load_decode)
  } .elsewhen (jalr_decode.op_code === op_code) {
    connectDecoders(jalr_decode)
  } .elsewhen (imm_decode.op_code === op_code) {
    connectDecoders(imm_decode)
  } .elsewhen (stores_decode.op_code === op_code) {
    connectDecoders(stores_decode)
  } .elsewhen (registers_decode.op_code === op_code) {
    connectDecoders(registers_decode)
  } .elsewhen (branches_decode.op_code === op_code) {
    connectDecoders(branches_decode)
  } .elsewhen (lui_decode.op_code === op_code) {
    connectDecoders(lui_decode)
  } .elsewhen (auipc_decode.op_code === op_code) {
    connectDecoders(auipc_decode)
  } .elsewhen (jal_decode.op_code === op_code) {
    connectDecoders(jal_decode)
  } .otherwise {
    //////////////////////////////////////////////////////
    // Invalid Instruction executes an architecture
    // specific NOP, and sets trap to 1
    //////////////////////////////////////////////////////
    io.registers.setDefaults
    io.alu.setDefaults
    io.pc.setDefaults
    io.mem.setDefaults
    io.sel_rf_wb     := DontCare
    io.sel_operand_a := DontCare
    io.trap          := true.B
  }

}
