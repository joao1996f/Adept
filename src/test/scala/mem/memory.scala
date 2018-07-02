package adept.mem

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.config.AdeptConfig
import adept.mem.tests._

import scala.collection.mutable.HashMap

class StallLogic(c: Memory) extends PeekPokeTester(c) {
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

class BaseMemory(c: Memory, config: AdeptConfig) extends PeekPokeTester(c) {
  // Write garbage data to the memory using the load interface and create a
  // mirror of the memory so that the tester can operate over.
  def writeGarbage(c: Memory) : HashMap[Int, Int] = {
    val mem_img = new HashMap[Int, Int]

    poke(c.io.load.we, true)

    for (i <- 0 until 5000) {
      poke(c.io.load.addr_w, i)
      val garbage = BigInt(config.XLen, rnd)
      mem_img(i) = garbage.toInt
      for (j <- 0 until 4) {
        // This is a Vec(4, 8.W)
        val shift = 8 * j
        poke(c.io.load.data_in(j), (garbage & (0x000000ff << shift)) >> shift)
      }
      step(1)
    }

    poke(c.io.load.we, false)
    step(1)

    return mem_img
  }

  // Initialize memory with garbage data
  val mem_img = writeGarbage(c)
}


class MemoryUnitTester(c: Memory, config: AdeptConfig) extends PeekPokeTester(c) {
  new StallLogic(c)
  new LoadByte(c, config)
  new LoadHalf(c, config)
  new LoadWord(c, config)

  new StoreByte(c, config)
  new StoreHalf(c, config)
  new StoreWord(c, config)
}

class MemoryTester extends ChiselFlatSpec {
  // Generate configuration
  val config = new AdeptConfig

  // Don't test in FIRRTL because it can't simulate a Verilog module

  "Memory" should s"tests stalling logic (with verilator)" in {
    Driver(() => new Memory(config), "verilator") {
      c => new StallLogic(c)
    } should be (true)
  }

  ///////////////////////////////////////////////////////////
  // Loads
  ///////////////////////////////////////////////////////////
  "Memory" should s"tests byte loads (with verilator)" in {
    Driver(() => new Memory(config), "verilator") {
      c => new LoadByte(c, config)
    } should be (true)
  }
  "Memory" should s"tests half loads (with verilator)" in {
    Driver(() => new Memory(config), "verilator") {
      c => new LoadHalf(c, config)
    } should be (true)
  }
  "Memory" should s"tests word loads (with verilator)" in {
    Driver(() => new Memory(config), "verilator") {
      c => new LoadWord(c, config)
    } should be (true)
  }
  "Memory" should s"tests byte unsigned loads (with verilator)" in {
    Driver(() => new Memory(config), "verilator") {
      c => new LoadByteUnsigned(c, config)
    } should be (true)
  }
  "Memory" should s"tests half unsigned loads (with verilator)" in {
    Driver(() => new Memory(config), "verilator") {
      c => new LoadHalfUnsigned(c, config)
    } should be (true)
  }

  ///////////////////////////////////////////////////////////
  // Stores
  ///////////////////////////////////////////////////////////
  "Memory" should s"tests byte stores (with verilator)" in {
    Driver(() => new Memory(config), "verilator") {
      c => new StoreByte(c, config)
    } should be (true)
  }
  "Memory" should s"tests half stores (with verilator)" in {
    Driver(() => new Memory(config), "verilator") {
      c => new StoreHalf(c, config)
    } should be (true)
  }
  "Memory" should s"tests word stores (with verilator)" in {
    Driver(() => new Memory(config), "verilator") {
      c => new StoreWord(c, config)
    } should be (true)
  }

}
