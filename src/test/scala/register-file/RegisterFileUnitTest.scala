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
    poke(c.io.decoder.we, 1)
    step(1)

    // Write B
    val b = rnd.nextInt(2^c.config.XLen)
    poke(c.io.rsd_value, b)
    poke(c.io.decoder.rsd_sel, sel + 1)
    poke(c.io.decoder.we, 1)
    step(1)

    // Read results
    poke(c.io.decoder.rs1_sel, sel)
    poke(c.io.decoder.rs2_sel, sel + 1)
    step(1)
    if (sel != 0) {
      expect(c.io.registers.rs1, a)
    }
    if ((sel + 1) != 0) {
      expect(c.io.registers.rs2, b)
    }
  }
}

class RegisterFileR0UnitTester(c: RegisterFile) extends PeekPokeTester(c) {
  // Write A
  poke(c.io.rsd_value, 0xdeadbeef)
  poke(c.io.decoder.rsd_sel, 0)
  poke(c.io.decoder.we, 1)
  step(1)

  // Write B
  poke(c.io.rsd_value, 0xbabebabe)
  poke(c.io.decoder.rsd_sel, 0)
  poke(c.io.decoder.we, 1)
  step(1)

  // Read result
  poke(c.io.decoder.rs1_sel, 0)
  expect(c.io.registers.rs1, 0)
}

class RegisterFileTester extends ChiselFlatSpec {
  // Generate configuration
  val config = new AdeptConfig(false,false,false)

  private val backendNames = Array("firrtl", "verilator")

  for ( backendName <- backendNames ) {
    "RegisterFile" should s"allow stores to register 0 (with $backendName)" in {
      Driver(() => new RegisterFile(config), backendName) {
        e => new RegisterFileR0UnitTester(e)
      } should be (true)
    }
    "RegisterFile" should s"store and read random variables (with $backendName)" in {
      Driver(() => new RegisterFile(config), backendName) {
        e => new RegisterFileUnitTester(e)
      } should be (true)
    }
  }
}
