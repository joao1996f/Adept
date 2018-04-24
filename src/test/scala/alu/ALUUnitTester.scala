package adept.alu

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.config.AdeptConfig
import adept.alu.imm._
import adept.alu.reg._
import adept.alu.br._

class ALUUnitTesterAll(e: ALU) extends PeekPokeTester(e) {
    // Immediate Type Instructions
    new ADDI(e)
    new SLTI(e)
    new SLTIU(e)
    new XORI(e)
    new ORI(e)
    new ANDI(e)
    new SLLI(e)
    new SRLI(e)
    new SRAI(e)

    // Register Type Instructions
    new ADD(e)
    new SUB(e)
    new SLL(e)
    new SLT(e)
    new SLTU(e)
    new XOR(e)
    new SRL(e)
    new SRA(e)
    new OR(e)
    new AND(e)

    // Branch Type Instructions
    new BEQ_BNE(e)
    new BLT_BGE(e)
    new BLTU_BGEU(e)
}

class ALUTester extends ChiselFlatSpec {
  // Generate configuration
  val config = new AdeptConfig

  private val backendNames = Array("firrtl", "verilator")

  for ( backendName <- backendNames ) {
    ////////////////////////////////////////////////////////////////////////////
    // Immediate Type Instructions
    ////////////////////////////////////////////////////////////////////////////
    "ALU" should s"test ADDI instruction (with $backendName)" in {
      Driver(() => new ALU(config), backendName) {
        e => new ADDI(e)
      } should be (true)
    }
    "ALU" should s"test SLTI instruction (with $backendName)" in {
      Driver(() => new ALU(config), backendName) {
        e => new SLTI(e)
      } should be (true)
    }
    "ALU" should s"test SLTIU instruction (with $backendName)" in {
      Driver(() => new ALU(config), backendName) {
        e => new SLTIU(e)
      } should be (true)
    }
    "ALU" should s"test XORI instruction (with $backendName)" in {
      Driver(() => new ALU(config), backendName) {
        e => new XORI(e)
      } should be (true)
    }
    "ALU" should s"test ORI instruction (with $backendName)" in {
      Driver(() => new ALU(config), backendName) {
        e => new ORI(e)
      } should be (true)
    }
    "ALU" should s"test ANDI instruction (with $backendName)" in {
      Driver(() => new ALU(config), backendName) {
        e => new ANDI(e)
      } should be (true)
    }
    "ALU" should s"test SLLI instruction (with $backendName)" in {
      Driver(() => new ALU(config), backendName) {
        e => new SLLI(e)
      } should be (true)
    }
    "ALU" should s"test SRLI instruction (with $backendName)" in {
      Driver(() => new ALU(config), backendName) {
        e => new SRLI(e)
      } should be (true)
    }
    "ALU" should s"test SRAI instruction (with $backendName)" in {
      Driver(() => new ALU(config), backendName) {
        e => new SRAI(e)
      } should be (true)
    }

    ////////////////////////////////////////////////////////////////////////////
    // Register Type Instructions
    ////////////////////////////////////////////////////////////////////////////
    "ALU" should s"test ADD instruction (with $backendName)" in {
      Driver(() => new ALU(config), backendName) {
        e => new ADD(e)
      } should be (true)
    }
    "ALU" should s"test SUB instruction (with $backendName)" in {
      Driver(() => new ALU(config), backendName) {
        e => new SUB(e)
      } should be (true)
    }
    "ALU" should s"test SLL instruction (with $backendName)" in {
      Driver(() => new ALU(config), backendName) {
        e => new SLL(e)
      } should be (true)
    }
    "ALU" should s"test SLT instruction (with $backendName)" in {
      Driver(() => new ALU(config), backendName) {
        e => new SLT(e)
      } should be (true)
    }
    "ALU" should s"test SLTU instruction (with $backendName)" in {
      Driver(() => new ALU(config), backendName) {
        e => new SLTU(e)
      } should be (true)
    }
    "ALU" should s"test XOR instruction (with $backendName)" in {
      Driver(() => new ALU(config), backendName) {
        e => new XOR(e)
      } should be (true)
    }
    "ALU" should s"test SRL instruction (with $backendName)" in {
      Driver(() => new ALU(config), backendName) {
        e => new SRL(e)
      } should be (true)
    }
    "ALU" should s"test SRA instruction (with $backendName)" in {
      Driver(() => new ALU(config), backendName) {
        e => new SRA(e)
      } should be (true)
    }
    "ALU" should s"test OR instruction (with $backendName)" in {
      Driver(() => new ALU(config), backendName) {
        e => new OR(e)
      } should be (true)
    }
    "ALU" should s"test AND instruction (with $backendName)" in {
      Driver(() => new ALU(config), backendName) {
        e => new AND(e)
      } should be (true)
    }
    ////////////////////////////////////////////////////////////////////////////
    // Branch Type Instructions
    ////////////////////////////////////////////////////////////////////////////
    "ALU" should s"test BEQ and BNE instructions (with $backendName)" in {
      Driver(() => new ALU(config), backendName) {
        e => new BEQ_BNE(e)
      } should be (true)
    }
    "ALU" should s"test BLT and BGE instructions (with $backendName)" in {
      Driver(() => new ALU(config), backendName) {
        e => new BLT_BGE(e)
      } should be (true)
    }
    "ALU" should s"test BLTU and BGEU instructions (with $backendName)" in {
      Driver(() => new ALU(config), backendName) {
        e => new BLTU_BGEU(e)
      } should be (true)
    }
  }
}
