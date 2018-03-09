package adept.instructionMemoryRom

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig

/*
 * Rom memory implemunted using vector
 * Needs to be filled with instructions 
*/

class InstrMemRom (config: AdeptConfig) extends Module {
  val io = IO (new Bundle{
    val in_pc = Input (UInt (config.XLen.W))
    val instr = Output (UInt (config.XLen.W))
  })
  val mem = Vec (Array (/*Instructions*/0.U))
  io.instr := mem (io.in_pc)
}

