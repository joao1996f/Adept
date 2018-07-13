// See LICENSE for license details.
package adept.idecode

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig
import adept.mem.DecoderMemIO
import adept.alu.DecoderAluIO
import adept.registerfile.DecoderRegisterFileIO
import adept.pc.DecoderPcIO

class InstructionDecoderOutput(config: AdeptConfig) extends Bundle {
  val registers = new DecoderRegisterFileIO(config)
  val alu       = new DecoderAluIO(config)
  val mem       = new DecoderMemIO(config)
  val pc        = new DecoderPcIO(config)

  // ALU selection control signals
  val sel_operand_a = UInt(1.W)
  // Write Back selection signals
  val sel_rf_wb     = UInt(1.W)

  // Trap
  val trap          = Bool()

  override def cloneType: this.type = {
    new InstructionDecoderOutput(config).asInstanceOf[this.type]
  }
}

class InstructionDecoder(config: AdeptConfig) extends Module {
  val io = IO(new Bundle{
                // Input
                val instruction = Input(UInt(config.XLen.W))
                val stall_reg   = Input(Bool())

                // Output
                val out = Output(new InstructionDecoderOutput(config))
              })

  // Ignore current instruction when the previous was a control instruction
  val instruction = Mux(io.stall_reg,
                        "h_0000_0013".U,
                        io.instruction)
  val op_code = instruction(6, 0)

  // Connects the wires of an instruction implementation to the output of the
  // Decoder
  def connectDecoders[T <: InstructionControlSignals](decoder: T) = {
    io.out.registers     := decoder.io.registers
    io.out.alu           := decoder.io.alu
    io.out.mem           := decoder.io.mem
    io.out.pc            := decoder.io.pc
    io.out.sel_operand_a := decoder.io.sel_operand_a
    io.out.sel_rf_wb     := decoder.io.sel_rf_wb
    io.out.trap          := decoder.io.trap
  }

  //////////////////////////////////////////////////////
  // I-Type Decode
  //////////////////////////////////////////////////////
  val load_decode = new LoadControlSignals(config, instruction, new InstructionDecoderOutput(config))
  val jalr_decode = new JalRControlSignals(config, instruction, new InstructionDecoderOutput(config))
  val imm_decode  = new ImmediateControlSignals(config, instruction, new InstructionDecoderOutput(config))

  //////////////////////////////////////////////////////
  // R-Type Decode
  //////////////////////////////////////////////////////
  val registers_decode = new RegisterControlSignals(config, instruction, new InstructionDecoderOutput(config))

  //////////////////////////////////////////////////////
  // S-Type Decode
  //////////////////////////////////////////////////////
  val stores_decode = new StoresControlSignals(config, instruction, new InstructionDecoderOutput(config))

  //////////////////////////////////////////////////////
  // B-Type Decode
  //////////////////////////////////////////////////////
  val branches_decode = new BranchesControlSignals(config, instruction, new InstructionDecoderOutput(config))

  //////////////////////////////////////////////////////
  // U-Type Decode
  //////////////////////////////////////////////////////
  val lui_decode = new LUIControlSignals(config, instruction, new InstructionDecoderOutput(config))
  val auipc_decode = new AUIPCControlSignals(config, instruction, new InstructionDecoderOutput(config))

  //////////////////////////////////////////////////////
  // J-Type Decode
  //////////////////////////////////////////////////////
  val jal_decode = new JALControlSignals(config, instruction, new InstructionDecoderOutput(config))

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
    io.out.registers.setDefaults
    io.out.alu.setDefaults
    io.out.pc.setDefaults
    io.out.mem.setDefaults
    io.out.sel_rf_wb     := DontCare
    io.out.sel_operand_a := DontCare
    io.out.trap          := true.B
  }

}
