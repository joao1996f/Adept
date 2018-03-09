package adept.instructionMemory

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.config.AdeptConfig
import adept.instructionMemory.MemoryDim

class InstrMemUnitTester(d: InstrMem) extends PeekPokeTester(d) {
  var MemData = Vector.fill(1024)(0)
  // Misses algorithm to fill the MemData 
  for (i <- 0 until 20){
    val pc = rnd.nextInt(1024)
    poke(d.io.in_pc, pc)
    step (1)
    expect(d.io.instr, MemData(pc))
  }
}

/**
* This is a trivial example of how to run this Specification
* From within sbt use:
* {{{
* testOnly adept.instructionmemory.InstrMemTester -- -z Basic
* }}}
* From a terminal shell use:
* {{{
* sbt 'testOnly adept.instructionmemory.InstrMemTester -- -z Basic'
* }}}
*/
class InstrMemTester extends ChiselFlatSpec {
  // Generate InstrMem configuration
  val config = new AdeptConfig
  val dimMem = new MemoryDim

  private val backendNames = if(firrtl.FileUtils.isCommandAvailable("verilator")) {
    Array("firrtl", "verilator")
  }
  else {
    Array("firrtl")
  }
  for ( backendName <- backendNames ) {
    "InstrMem" should s"store random data (with $backendName)" in {
      Driver(() => new InstrMem(config, dimMem), backendName) {
        d => new InstrMemUnitTester(d)
      } should be (true)
    }
  }

  "Basic test using Driver.execute" should "be used as an alternative way to run specification" in {
    iotesters.Driver.execute(Array(), () => new InstrMem(config, dimMem)) {
      d => new InstrMemUnitTester(d)
    } should be (true)
  }

  "using --backend-name verilator" should "be an alternative way to run using verilator" in {
    if(backendNames.contains("verilator")) {
      iotesters.Driver.execute(Array("--backend-name", "verilator"), () => new InstrMem(config, dimMem)) {
        d => new InstrMemUnitTester(d)
      } should be(true)
    }
  }

  "running with --is-verbose" should "show more about what's going on in your tester" in {
    iotesters.Driver.execute(Array("--is-verbose"), () => new InstrMem(config, dimMem)) {
      d => new InstrMemUnitTester(d)
    } should be(true)
  }

  "running with --fint-write-vcd" should "create a vcd file from your test" in {
    iotesters.Driver.execute(Array("--fint-write-vcd"), () => new InstrMem(config, dimMem)) {
      d => new InstrMemUnitTester(d)
    } should be(true)
  }

  "using --help" should s"show the many options available" in {
    iotesters.Driver.execute(Array("--help"), () => new InstrMem(config, dimMem)) {
      d => new InstrMemUnitTester(d)
    } should be (true)
  }
}
