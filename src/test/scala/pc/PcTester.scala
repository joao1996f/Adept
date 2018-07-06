package adept.pc

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.config.AdeptConfig
import adept.pc.tests._

// TODO: Add tests which take into account the stalls coming from the memory.
class PcUnitTester(c: Pc) extends PeekPokeTester(c) {
  new BEQ(c)
  reset(2)
  new BNE(c)
  reset(2)
  new BLT(c)
  reset(2)
  new BGE(c)
  reset(2)
  new BLTU(c)
  reset(2)
  new BGEU(c)
  reset(2)
  new JAL(c)
  reset(2)
  new JALR(c)
}

class PcTester extends ChiselFlatSpec {
  // Generate configuration
  val config = new AdeptConfig(false)
  val branch_config = new BranchOpConstants

  private val backendNames = Array("firrtl", "verilator")

  for ( backendName <- backendNames ) {
    ///////////////////////////////////////////
    // Branches
    ///////////////////////////////////////////
    "PC" should s"test BEQ operations (with $backendName)" in {
      Driver(() => new Pc(config, branch_config), backendName) {
        c => new BEQ(c)
      } should be (true)
    }
    "PC" should s"test BNE operations (with $backendName)" in {
      Driver(() => new Pc(config, branch_config), backendName) {
        c => new BNE(c)
      } should be (true)
    }
    "PC" should s"test BLT operations (with $backendName)" in {
      Driver(() => new Pc(config, branch_config), backendName) {
        c => new BLT(c)
      } should be (true)
    }
    "PC" should s"test BGE operations (with $backendName)" in {
      Driver(() => new Pc(config, branch_config), backendName) {
        c => new BGE(c)
      } should be (true)
    }
    "PC" should s"test BLTU operations (with $backendName)" in {
      Driver(() => new Pc(config, branch_config), backendName) {
        c => new BLTU(c)
      } should be (true)
    }
    "PC" should s"test BGEU operations (with $backendName)" in {
      Driver(() => new Pc(config, branch_config), backendName) {
        c => new BGEU(c)
      } should be (true)
    }

    ///////////////////////////////////////////
    // Jumps
    ///////////////////////////////////////////
    "PC" should s"test JAL operations (with $backendName)" in {
      Driver(() => new Pc(config, branch_config), backendName) {
        c => new JAL(c)
      } should be (true)
    }
    "PC" should s"test JALR operations (with $backendName)" in {
      Driver(() => new Pc(config, branch_config), backendName) {
        c => new JALR(c)
      } should be (true)
    }
  }
}
