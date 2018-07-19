package adept.idecode.integer

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig
import adept.idecode.InstructionDecoderOutput

class IntegerDecoder(val config: AdeptConfig, instruction: UInt) {
  //////////////////////////////////////////////////////
  // I-Type Decode
  //////////////////////////////////////////////////////
  private val load_decode = new LoadControlSignals(config, instruction, new InstructionDecoderOutput(config))
  private val jalr_decode = new JalRControlSignals(config, instruction, new InstructionDecoderOutput(config))
  private val imm_decode  = new ImmediateControlSignals(config, instruction, new InstructionDecoderOutput(config))

  //////////////////////////////////////////////////////
  // R-Type Decode
  //////////////////////////////////////////////////////
  private val registers_decode = new RegisterControlSignals(config, instruction, new InstructionDecoderOutput(config))

  //////////////////////////////////////////////////////
  // S-Type Decode
  //////////////////////////////////////////////////////
  private val stores_decode = new StoresControlSignals(config, instruction, new InstructionDecoderOutput(config))

  //////////////////////////////////////////////////////
  // B-Type Decode
  //////////////////////////////////////////////////////
  private val branches_decode = new BranchesControlSignals(config, instruction, new InstructionDecoderOutput(config))

  //////////////////////////////////////////////////////
  // U-Type Decode
  //////////////////////////////////////////////////////
  private val lui_decode = new LUIControlSignals(config, instruction, new InstructionDecoderOutput(config))
  private val auipc_decode = new AUIPCControlSignals(config, instruction, new InstructionDecoderOutput(config))

  //////////////////////////////////////////////////////
  // J-Type Decode
  //////////////////////////////////////////////////////
  private val jal_decode = new JALControlSignals(config, instruction, new InstructionDecoderOutput(config))

  // Build Decoder
  val decoder = Array(
    load_decode.op_code      -> load_decode.io,
    jalr_decode.op_code      -> jalr_decode.io,
    imm_decode.op_code       -> imm_decode.io,
    stores_decode.op_code    -> stores_decode.io,
    registers_decode.op_code -> registers_decode.io,
    branches_decode.op_code  -> branches_decode.io,
    lui_decode.op_code       -> lui_decode.io,
    auipc_decode.op_code     -> auipc_decode.io,
    jal_decode.op_code       -> jal_decode.io
  )
}
