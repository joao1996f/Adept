package adept.mem

import scala.collection.mutable.HashMap

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.config.AdeptConfig

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
}

class Load(c: Memory, config: AdeptConfig) extends BaseMemory(c, config) {
  private def LB(addr: Int) = {
    poke(c.io.decode.op, 0)
    poke(c.io.in.addr, addr)
    poke(c.io.decode.en, true)
    poke(c.io.decode.we, false)

    // Ignore output while stall is active and advance simulation
    do {
      step(1)
    } while (peek(c.io.stall) == 1)

    val lsbs = addr & 0x00000003
    val masked_addr = addr >>> 2
    val final_read = lsbs match {
      case 0 => mem_img(masked_addr) & 0x000000ff
      case 1 => (mem_img(masked_addr) & 0x0000ff00) >>> 8
      case 2 => (mem_img(masked_addr) & 0x00ff0000) >>> 16
      case 3 => (mem_img(masked_addr) & 0xff000000) >>> 24
      case _ => 0
    }
    val sign_extend = if (((final_read & 0x00000080) >>> 7) == 1) {
      0xffffff00
    } else {
      0x00000000
    }

    expect(c.io.data_out, sign_extend | final_read)
  }

  // Initialize memory with garbage data
  val mem_img = writeGarbage(c)

  for (i <- 0 until 100) {
    val addr = rnd.nextInt(5000)
    LB(addr)
  }
}

class MemoryUnitTester(c: Memory, config: AdeptConfig) extends PeekPokeTester(c) {
  // new StallLogic(c)
  new Load(c, config)
}

class MemoryTester extends ChiselFlatSpec {
  // Generate configuration
  val config = new AdeptConfig

  private val backendNames = Array("firrtl", "verilator")

  for ( backendName <- backendNames ) {
    // "Memory" should s"tests stalling logic (with $backendName)" in {
    //   Driver(() => new Memory(config), backendName) {
    //     c => new StallLogic(c)
    //   } should be (true)
    // }
    "Memory" should s"tests loads (with $backendName)" in {
      Driver(() => new Memory(config), backendName) {
        c => new Load(c, config)
      } should be (true)
    }
  }
}
