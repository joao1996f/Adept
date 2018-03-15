package adept.instructionMemoryRom

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig

/*
 * ROM memory implemented using a vector.
 * Needs to be filled with instructions.
*/

class InstrMemRom (config: AdeptConfig) extends Module {
  val io = IO (new Bundle{
    val in_pc = Input(UInt(config.XLen.W))
    val instr = Output(UInt(config.XLen.W))
  })
  val mem = Vec(Array(
  "hf9010113".U,
 "h06812623".U,
 "h07010413".U,
 "hf8042e23".U,
 "h00100793".U,
 "hfaf42023".U,
 "h00200793".U,
 "hfef42623".U,
 "h0580006f".U,
 "hfec42783".U,
 "hffe78793".U,
 "h00279793".U,
 "hff040713".U,
 "h00f707b3".U,
 "hfac7a703".U,
 "hfec42783".U,
 "hfff78793".U,
 "h00279793".U,
 "hff040693".U,
 "h00f687b3".U,
 "hfac7a783".U,
 "h00f70733".U,
 "hfec42783".U,
 "h00279793".U,
 "hff040693".U,
 "h00f687b3".U,
 "hfae7a623".U,
 "hfec42783".U,
 "h00178793".U,
 "hfef42623".U,
 "hfec42703".U,
 "h01300793".U,
 "hfae7d2e3".U,
 "h00000013".U,
 "h06c12403".U,
 "h07010113".U,
 "h00008067".U))
 io.instr := mem(io.in_pc)
}
