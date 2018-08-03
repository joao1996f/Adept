package adept.decoder

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.config.AdeptConfig

import adept.decoder.tests.imm._
import adept.decoder.tests.reg._

class DecoderTestBase(c: InstructionDecoder) extends PeekPokeTester(c) {
  val op_code = new OpCodes
  val sll = Integer.parseInt("001", 2)
  val slt = Integer.parseInt("010", 2)
  val funct7alu = Integer.parseInt("0100000", 2);

  def signExtension(imm: Int, nbits: Int) : Int = {
    if ((imm >> (nbits - 1)) == 1) {
      ((0xFFFFFFFF << nbits) | imm)
    } else {
      imm
    }
  }
}

class DecoderUnitTesterAll(e: InstructionDecoder) extends PeekPokeTester(e) {
    // Immediate Type Instructions
    new ADDI(e)
    new SLTI(e)
    new SLLI(e)

    // Register Type Instructions
    new ADD(e)
    new SUB(e)
    new SLL(e)
}

class DecoderTester extends ChiselFlatSpec {
  // Generate configuration
  val config = new AdeptConfig

  ///////////////////////////////////////////////////////////////////////////
  // Immediate Type Instructions
  ////////////////////////////////////////////////////////////////////////////
  /*"Decoder" should s"test ADDI instruction (with verilator)" in {
    Driver(() => new InstructionDecoder(config), "verilator") {
      e => new ADDI(e)
    } should be (true)
  }
  "Decoder" should s"test SLTI instruction (with verilator)" in {
    Driver(() => new InstructionDecoder(config), "verilator") {
      e => new SLTI(e)
    } should be (true)
  }
  "Decoder" should s"test SLLI instruction (with verilator)" in {
    Driver(() => new InstructionDecoder(config), "verilator") {
      e => new SLLI(e)
    } should be (true)
  }*/

  ////////////////////////////////////////////////////////////////////////////
  // Register Type Instructions
  ////////////////////////////////////////////////////////////////////////////
  "Decoder" should s"test ADD instruction (with verilator)" in {
    Driver(() => new InstructionDecoder(config), "verilator") {
      e => new ADD(e)
    } should be (true)
  }
  "Decoder" should s"test SUB instruction (with verilator)" in {
    Driver(() => new InstructionDecoder(config), "verilator") {
      e => new SUB(e)
    } should be (true)
  }
  "Decoder" should s"test SLL instruction (with verilator)" in {
    Driver(() => new InstructionDecoder(config), "verilator") {
      e => new SLL(e)
    } should be (true)
  }
}
