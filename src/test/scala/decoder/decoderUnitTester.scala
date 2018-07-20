package adept.decoder

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.config.AdeptConfig

import adept.decoder.tests.imm._

class DecoderTestBase(c: InstructionDecoder) extends PeekPokeTester(c) {
  val op_code = new OpCodes
  val slli = 1  //b001
}

class DecoderUnitTesterAll(e: InstructionDecoder) extends PeekPokeTester(e) {
    // Immediate Type Instructions
    new SLLI(e)
}

class DecoderTester extends ChiselFlatSpec {
  // Generate configuration
  val config = new AdeptConfig
  
  ///////////////////////////////////////////////////////////////////////////
  // Immediate Type Instructions
  ////////////////////////////////////////////////////////////////////////////
  "Decoder" should s"test SLLI instruction (with verilator)" in {
    Driver(() => new InstructionDecoder(config), "verilator") {
      e => new SLLI(e)
    } should be (true)
  }
}
