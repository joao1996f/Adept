package adept.pc.tests

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.config.AdeptConfig
import adept.pc.Pc

class Jump(c: Pc) extends ControlCommon(c) {
  object OpCode extends Enumeration {
    type OpCode = Value
    val JALR = Value(Integer.parseInt("1100111", 2))
    val JAL  = Value(Integer.parseInt("1101111", 2))
  }
  import OpCode._

  def evalBranch(op: OpCode, offset: BigInt) : BigInt = {
    op match {
      case JAL => return my_pc + offset
      case JALR => return offset & 0xFFFFFFFE
    }
  }

  def testBranch(offset: BigInt, opcode: OpCode, func: Int = 0) = {
    setBranchSignals(opcode.id, offset, func)
    my_pc = evalBranch(opcode, offset)

    step(1)
    branchHazardStall(1, true)

    expectBranchSignals(false)
  }
}

class JAL(c: Pc) extends Jump(c) {
  for (i <- 0 until 100) {
    val imm = rnd.nextInt(0x1FFFFF) & (0x1FFFFF - 1)
    val offset = imm | getSignExtend(imm, 0x100000)

    testBranch(offset, OpCode.JAL)
  }
}

class JALR(c: Pc) extends Jump(c) {
  for (i <- 0 until 100) {
    val rs1 = BigInt(32, rnd)
    val imm = rnd.nextInt(0xFFF)
    val offset = imm | getSignExtend(imm, 0x800)

    testBranch(offset + rs1, OpCode.JALR, Integer.parseInt("000", 2))
  }
}
