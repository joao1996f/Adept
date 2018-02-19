package adept

import chisel3._
import chisel3.util.log2Ceil

import adept.config.AdeptConfig
import adept.idecode._

class Adept(config: AdeptConfig) extends Module {

  // Create Modules
  // Instruction Decoder
  val idecode = Module(new InstructionDecode(config))

  // Register File
  // Creates a register file with 32 32-bit registers
  val register_file = Module(new RegisterFile(config.XLen, config.XLen))
}
