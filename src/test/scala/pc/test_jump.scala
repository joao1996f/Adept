package adept.pc.tests

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.config.AdeptConfig
import adept.pc.Pc

class Jump(c: Pc) extends ControlCommon(c) {
  def testBranch(offset: BigInt, op: BigInt, rs1: BigInt = 0) = {
    setBranchSignals(op, offset, rs1 = rs1)
    my_pc = evalBranch(op, offset, rs1)

    step(1)
    branchHazardStall(1, true)

    expectBranchSignals(false)
  }
}

class JAL(c: Pc) extends Jump(c) {
  for (i <- 0 until 100) {
    val imm = rnd.nextInt(0x1FFFFF) & (0x1FFFFF - 1)
    val offset = imm | getSignExtend(imm, 0x100000)

    testBranch(offset, pc_ops.jal.litValue())
  }
}

class JALR(c: Pc) extends Jump(c) {
  for (i <- 0 until 100) {
    val rs1 = BigInt(32, rnd)
    val imm = rnd.nextInt(0xFFF)
    val offset = getSignExtend(imm, 0x800) | imm

    testBranch(offset, pc_ops.jalr.litValue(), rs1)
  }
}
