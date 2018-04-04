package adept.mem

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.config.AdeptConfig

// This memory tester only tests the stall mechanism and will ignore any write
// and reads performed to the cache.
class MemoryUnitTester(c: Memory) extends PeekPokeTester(c) {
  var counter = 0
  val n_iter = rnd.nextInt(1000) * 59
  var en = rnd.nextInt(2) == 1
  var wait_result = false
  var first = true

  for (i <- 0 until n_iter) {
    poke(c.io.decode.en, en)

    step(1)

    if (en && !wait_result) {
      // println("Counter (Test): " + counter)
      counter += 1
      if (counter == 59 && first) {
        counter = 0
        wait_result = true
        first = false
      } else if (counter == 56 && !first) {
        counter = 0
        wait_result = true
      }
    }

    if (wait_result && en) {
      expect(c.io.stall, false)
      wait_result = false
    } else if (en) {
      expect(c.io.stall, true)
    }

    if (!en) {
      // Generate new enable
      en = rnd.nextInt(2) == 1
    }
  }
}

/**
* This is a trivial example of how to run this Specification
* From within sbt use:
* {{{
* testOnly adept.core.MemoryTester -- -z Basic
* }}}
* From a terminal shell use:
* {{{
* sbt 'testOnly adept.core.MemoryTester -- -z Basic'
* }}}
*/
class MemoryTester extends ChiselFlatSpec {
  // Generate Pc configuration
  val config = new AdeptConfig

  private val backendNames = if(firrtl.FileUtils.isCommandAvailable("verilator")) {
    Array("firrtl", "verilator")
  }
  else {
    Array("firrtl")
  }
  for ( backendName <- backendNames ) {
    "Memory" should s"store random data (with $backendName)" in {
      Driver(() => new Memory(config), backendName) {
        e => new MemoryUnitTester(e)
      } should be (true)
    }
  }

  "Basic test using Driver.execute" should "be used as an alternative way to run specification" in {
    iotesters.Driver.execute(Array(), () => new Memory(config )) {
      e => new MemoryUnitTester(e)
    } should be (true)
  }

  "using --backend-name verilator" should "be an alternative way to run using verilator" in {
    if(backendNames.contains("verilator")) {
      iotesters.Driver.execute(Array("--backend-name", "verilator"), () => new Memory(config )) {
        e => new MemoryUnitTester(e)
      } should be(true)
    }
  }

  "running with --is-verbose" should "show more about what's going on in your tester" in {
    iotesters.Driver.execute(Array("--is-verbose"), () => new Memory(config )) {
      e => new MemoryUnitTester(e)
    } should be(true)
  }

  "running with --fint-write-vcd" should "create a vcd file from your test" in {
    iotesters.Driver.execute(Array("--fint-write-vcd"), () => new Memory(config )) {
      e => new MemoryUnitTester(e)
    } should be(true)
  }

  "using --help" should s"show the many options available" in {
    iotesters.Driver.execute(Array("--help"), () => new Memory(config)) {
      e => new MemoryUnitTester(e)
    } should be (true)
  }
}
