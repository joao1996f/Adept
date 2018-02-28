// ALU Unit Tester
package adept.alu

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.config.AdeptConfig

/*
 * ALU Unit Tester
 *
 * The unit tester will run through all ALU pertinent instructions to check for
 * their correct functionality. Each test should note what each input is doing,
 * and why it's important, followed by its expected output. In the case of an
 * edge case test, the author should provide a detailed explanation of why their
 * set of inputs fails to achieve the correct result, and why their fix works.
 * If need be, please quote the ISA specification.
 */
class ALUUnitTester(c: ALU) extends PeekPokeTester(c) {
  ////////////////////////////////////////////////
  // Immediate Type instructions
  ////////////////////////////////////////////////

  // ADDI
  // Simple Test: two positive values 5 + 1 = 6
  poke(c.io.rs1, 5)
  poke(c.io.imm, 1)
  poke(c.io.op, 0) // b000
  poke(c.io.op_code, 19) // b0010011
  step(1)
  expect(c.io.result, 6)
  // Positive rs1 and Negative immediate 5 + (-1) = 4
  poke(c.io.rs1, 5)
  poke(c.io.imm, -1)
  poke(c.io.op, 0) // b000
  poke(c.io.op_code, 19) // b0010011
  step(1)
  expect(c.io.result, 4)
  // Negative rs1 and Positive immediate -5 + 1 = -4
  poke(c.io.rs1, -5)
  poke(c.io.imm, 1)
  poke(c.io.op, 0) // b000
  poke(c.io.op_code, 19) // b0010011
  step(1)
  expect(c.io.result, -4)
  // Negative rs1 and Negative immediate -5 + (-1) = -6
  poke(c.io.rs1, -5)
  poke(c.io.imm, -1)
  poke(c.io.op, 0) // b000
  poke(c.io.op_code, 19) // b0010011
  step(1)
  expect(c.io.result, -6)
}

/**
* This is a trivial example of how to run this Specification
* From within sbt use:
* {{{
* testOnly chutils.test.RegisterFileTester
* }}}
* From a terminal shell use:
* {{{
* sbt 'testOnly chutils.test.RegisterFileTester'
* }}}
*/
class ALUTester extends ChiselFlatSpec {
  // Generate ALU configuration
  val config = new AdeptConfig

  private val backendNames = if(firrtl.FileUtils.isCommandAvailable("verilator")) {
    Array("firrtl", "verilator")
  }
  else {
    Array("firrtl")
  }
  for ( backendName <- backendNames ) {
    "ALU" should s"store random data (with $backendName)" in {
      Driver(() => new ALU(config), backendName) {
        c => new ALUUnitTester(c)
      } should be (true)
    }
  }

  "Basic test using Driver.execute" should "be used as an alternative way to run specification" in {
    iotesters.Driver.execute(Array(), () => new ALU(config)) {
      c => new ALUUnitTester(c)
    } should be (true)
  }

  "using --backend-name verilator" should "be an alternative way to run using verilator" in {
    if(backendNames.contains("verilator")) {
      iotesters.Driver.execute(Array("--backend-name", "verilator"), () => new ALU(config)) {
        c => new ALUUnitTester(c)
      } should be(true)
    }
  }

  "running with --is-verbose" should "show more about what's going on in your tester" in {
    iotesters.Driver.execute(Array("--is-verbose"), () => new ALU(config)) {
      c => new ALUUnitTester(c)
    } should be(true)
  }

  "running with --fint-write-vcd" should "create a vcd file from your test" in {
    iotesters.Driver.execute(Array("--fint-write-vcd"), () => new ALU(config)) {
      c => new ALUUnitTester(c)
    } should be(true)
  }

  "using --help" should s"show the many options available" in {
    iotesters.Driver.execute(Array("--help"), () => new ALU(config)) {
      c => new ALUUnitTester(c)
    } should be (true)
  }
}
