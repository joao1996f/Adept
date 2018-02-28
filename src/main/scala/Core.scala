package adept.core

import chisel3._
import chisel3.util.log2Ceil

import adept.config.AdeptConfig
import adept.idecode.InstructionDecoder
import adept.registerfile.RegisterFile

class Adept(config: AdeptConfig) extends Module {
  val io = IO(new Bundle{})

  // Create Modules
  // Instruction Decoder
  val idecode = Module(new InstructionDecoder(config))

  // Register File
  // Creates a register file with 32 32-bit registers
  val register_file = Module(new RegisterFile(config.XLen, config.XLen))
}
