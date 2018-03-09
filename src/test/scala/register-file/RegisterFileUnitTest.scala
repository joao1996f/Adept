package adept.registerfile

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.config.AdeptConfig

class RegisterFileUnitTester(c: RegisterFile) extends PeekPokeTester(c) {
  val n_tests = rnd.nextInt(1000) * 10

  for (i <- 0 until n_tests) {
    var sel = 0
    do {
      sel = rnd.nextInt(c.config.XLen)
    } while (sel == c.config.XLen - 1)

    // Write A
    val a = rnd.nextInt(2^c.config.XLen)
    poke(c.io.rsd_value, a)
    poke(c.io.decoder.rsd_sel, sel)
    poke(c.io.we, 1)
    step(1)

    // Write B
    val b = rnd.nextInt(2^c.config.XLen)
    poke(c.io.rsd_value, b)
    poke(c.io.decoder.rsd_sel, sel + 1)
    poke(c.io.we, 1)
    step(1)

    // Read results
    poke(c.io.decoder.rs1_sel, sel)
    poke(c.io.decoder.rs2_sel, sel + 1)
    step(1)
    expect(c.io.registers.rs1, a)
    expect(c.io.registers.rs2, b)
  }
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
class RegisterFileTester extends ChiselFlatSpec {
  // Can't use rnd here
  val config = new AdeptConfig

  private val backendNames = if(firrtl.FileUtils.isCommandAvailable("verilator")) {
    Array("firrtl", "verilator")
  }
  else {
    Array("firrtl")
  }
  for ( backendName <- backendNames ) {
    "RegisterFile" should s"store random data (with $backendName)" in {
      Driver(() => new RegisterFile(config), backendName) {
        c => new RegisterFileUnitTester(c)
      } should be (true)
    }
  }

  "Basic test using Driver.execute" should "be used as an alternative way to run specification" in {
    iotesters.Driver.execute(Array(), () => new RegisterFile(config)) {
      c => new RegisterFileUnitTester(c)
    } should be (true)
  }

  "using --backend-name verilator" should "be an alternative way to run using verilator" in {
    if(backendNames.contains("verilator")) {
      iotesters.Driver.execute(Array("--backend-name", "verilator"), () => new RegisterFile(config)) {
        c => new RegisterFileUnitTester(c)
      } should be(true)
    }
  }

  "running with --is-verbose" should "show more about what's going on in your tester" in {
    iotesters.Driver.execute(Array("--is-verbose"), () => new RegisterFile(config)) {
      c => new RegisterFileUnitTester(c)
    } should be(true)
  }

  "running with --fint-write-vcd" should "create a vcd file from your test" in {
    iotesters.Driver.execute(Array("--fint-write-vcd"), () => new RegisterFile(config)) {
      c => new RegisterFileUnitTester(c)
    } should be(true)
  }

  "using --help" should s"show the many options available" in {
    iotesters.Driver.execute(Array("--help"), () => new RegisterFile(config)) {
      c => new RegisterFileUnitTester(c)
    } should be (true)
  }
}
