package adept.core

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.config.AdeptConfig

class AdeptUnitTester(c: Adept) extends PeekPokeTester(c) {
  // Load program while core is in reset
  val program = Array()

  for ((data, addr) <- program.zipWithIndex) {
    poke(c.io.reset, true.B)
    poke(c.io.we, true.B)
    poke(c.io.data_in, data)
    poke(c.io.addr_w, addr)
    step(1)
  }

  // Lower reset and write_enable. Core should start processing
  poke(c.io.reset, false.B)
  poke(c.io.we, false.B)

  // Wait for success
  while(peek(c.io.success) === false.B) {
    step(1)
  }

}

/**
* This is a trivial example of how to run this Specification
* From within sbt use:
* {{{
* testOnly adept.pc.PcTester -- -z Basic
* }}}
* From a terminal shell use:
* {{{
* sbt 'testOnly adept.pc.PcTester -- -z Basic'
* }}}
*/
class AdeptTester extends ChiselFlatSpec {
  // Generate Pc configuration
  val config = new AdeptConfig

  private val backendNames = if(firrtl.FileUtils.isCommandAvailable("verilator")) {
    Array("firrtl", "verilator")
  }
  else {
    Array("firrtl")
  }
  for ( backendName <- backendNames ) {
    "Adept" should s"store random data (with $backendName)" in {
      Driver(() => new Adept(config), backendName) {
        e => new AdeptUnitTester(e)
      } should be (true)
    }
  }

  "Basic test using Driver.execute" should "be used as an alternative way to run specification" in {
    iotesters.Driver.execute(Array(), () => new Adept(config )) {
      e => new AdeptUnitTester(e)
    } should be (true)
  }

  "using --backend-name verilator" should "be an alternative way to run using verilator" in {
    if(backendNames.contains("verilator")) {
      iotesters.Driver.execute(Array("--backend-name", "verilator"), () => new Adept(config )) {
        e => new AdeptUnitTester(e)
      } should be(true)
    }
  }

  "running with --is-verbose" should "show more about what's going on in your tester" in {
    iotesters.Driver.execute(Array("--is-verbose"), () => new Adept(config )) {
      e => new AdeptUnitTester(e)
    } should be(true)
  }

  "running with --fint-write-vcd" should "create a vcd file from your test" in {
    iotesters.Driver.execute(Array("--fint-write-vcd"), () => new Adept(config )) {
      e => new AdeptUnitTester(e)
    } should be(true)
  }

  "using --help" should s"show the many options available" in {
    iotesters.Driver.execute(Array("--help"), () => new Adept(config)) {
      e => new AdeptUnitTester(e)
    } should be (true)
  }
}
