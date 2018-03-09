package adept.registerfile

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

class RegisterFileUnitTester(c: RegisterFile) extends PeekPokeTester(c) {
  val n_tests = rnd.nextInt(1000) * 10

  for (i <- 0 until n_tests) {
    var sel = 0
    do {
      sel = rnd.nextInt(c.n_regs)
    } while (sel == c.n_regs - 1)

    // Write A
    val a = rnd.nextInt(2^c.data_w)
    poke(c.io.rd_value, a)
    poke(c.io.rd_sel, sel)
    poke(c.io.we, 1)
    step(1)

    // Write B
    val b = rnd.nextInt(2^c.data_w)
    poke(c.io.rd_value, b)
    poke(c.io.rd_sel, sel + 1)
    poke(c.io.we, 1)
    step(1)

    // Read results
    poke(c.io.rs1_sel, sel)
    poke(c.io.rs2_sel, sel + 1)
    step(1)
    expect(c.io.rs1, a)
    expect(c.io.rs2, b)
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
  val data_w = scala.util.Random.nextInt(1024)
  val n_regs = scala.util.Random.nextInt(64)
  println("Data Width " + data_w)
  println("Number of Registers " + n_regs)

  private val backendNames = if(firrtl.FileUtils.isCommandAvailable("verilator")) {
    Array("firrtl", "verilator")
  }
  else {
    Array("firrtl")
  }
  for ( backendName <- backendNames ) {
    "RegisterFile" should s"store random data (with $backendName)" in {
      Driver(() => new RegisterFile(data_w, n_regs), backendName) {
        c => new RegisterFileUnitTester(c)
      } should be (true)
    }
  }

  "Basic test using Driver.execute" should "be used as an alternative way to run specification" in {
    iotesters.Driver.execute(Array(), () => new RegisterFile(data_w, n_regs)) {
      c => new RegisterFileUnitTester(c)
    } should be (true)
  }

  "using --backend-name verilator" should "be an alternative way to run using verilator" in {
    if(backendNames.contains("verilator")) {
      iotesters.Driver.execute(Array("--backend-name", "verilator"), () => new RegisterFile(data_w, n_regs)) {
        c => new RegisterFileUnitTester(c)
      } should be(true)
    }
  }

  "running with --is-verbose" should "show more about what's going on in your tester" in {
    iotesters.Driver.execute(Array("--is-verbose"), () => new RegisterFile(data_w, n_regs)) {
      c => new RegisterFileUnitTester(c)
    } should be(true)
  }

  "running with --fint-write-vcd" should "create a vcd file from your test" in {
    iotesters.Driver.execute(Array("--fint-write-vcd"), () => new RegisterFile(data_w, n_regs)) {
      c => new RegisterFileUnitTester(c)
    } should be(true)
  }

  "using --help" should s"show the many options available" in {
    iotesters.Driver.execute(Array("--help"), () => new RegisterFile(data_w, n_regs)) {
      c => new RegisterFileUnitTester(c)
    } should be (true)
  }
}
