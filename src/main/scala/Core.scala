package adept.core

import chisel3._
import chisel3.util.log2Ceil

import adept.config.AdeptConfig
import adept.idecode.InstructionDecoder
import adept.registerfile.RegisterFile
import adept.memory.Memory

class Adept(config: AdeptConfig) extends Module {
  val io = IO(new Bundle{})

  //////////////////////////////////////////////////////////////////////////////
  // Create Modules
  //////////////////////////////////////////////////////////////////////////////
  // Instruction Decoder
  val idecode = Module(new InstructionDecoder(config))

  // Register File
  // Creates a register file with XLen XLen-bit registers
  val register_file = Module(new RegisterFile(config))

  // ALU
  val alu = Module(new ALU(config))

  //////////////////////////////////////////////////////////////////////////////
  // Connections
  //////////////////////////////////////////////////////////////////////////////
  register_file.decoder <> idecode.registers
  alu.registers <> register_file.registers
  alu.decoder_params <> idecode.alu
}
