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

class MemoryTester extends ChiselFlatSpec {
  // Generate configuration
  val config = new AdeptConfig

  private val backendNames = Array("firrtl", "verilator")

  for ( backendName <- backendNames ) {
    "Memory" should s"store random data (with $backendName)" in {
      Driver(() => new Memory(config), backendName) {
        e => new MemoryUnitTester(e)
      } should be (true)
    }
  }
}
