package adept.pc.tests

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.config.AdeptConfig
import adept.pc.Pc

class Branch(c: Pc) extends ControlCommon(c) {
  val opcode = Integer.parseInt("1100011", 2)
  val max_offset = 0x1FFF
  val bitMask = 0x1000

  // Branch Functions
  object Func extends Enumeration {
    type Func = Value
    val BEQ  = Value(Integer.parseInt("000", 2))
    val BNE  = Value(Integer.parseInt("001", 2))
    val BLT  = Value(Integer.parseInt("100", 2))
    val BLTU = Value(Integer.parseInt("110", 2))
    val BGE  = Value(Integer.parseInt("101", 2))
    val BGEU = Value(Integer.parseInt("111", 2))
    val Empty = Value(Integer.parseInt("010", 2))
  }
  import Func._

  var func = Func.Empty

  def evalBranch(flag: Boolean, offset: Int) : BigInt = {
    func match {
      case BNE | BLT | BLTU if flag => return my_pc + offset
      case BEQ | BGE | BGEU if !flag =>  return my_pc + offset
      case _ => return my_pc + 4
    }
  }

  def genData(i: Int) : (Int, BigInt, BigInt) = {
    val rs1 = BigInt(32, rnd)
    val rs2 = if (i % 2 == 0) {
      BigInt(32, rnd)
    } else {
      rs1
    }

    val imm = rnd.nextInt(max_offset) & (max_offset - 1)
    val offset = imm | getSignExtend(imm, bitMask)

    return (offset, rs1, rs2)
  }

  def testBranch(offset: Int, flag: Boolean, hw_flag: (Boolean) => Boolean) = {
    setBranchSignals(opcode, offset, func.id, hw_flag(flag))
    my_pc = evalBranch(hw_flag(flag), offset)

    step(1)
    branchHazardStall(1, flag)

    expectBranchSignals(false)
  }
}

class BEQ(c: Pc) extends Branch(c) {
  func = Func.BEQ
  for (i <- 0 until 100) {
    val (offset, rs1, rs2) = genData(i)
    testBranch(offset, rs1 == rs2, (flag: Boolean) => !flag)
  }
}

class BNE(c: Pc) extends Branch(c) {
  func = Func.BNE
  for (i <- 0 until 100) {
    val (offset, rs1, rs2) = genData(i)
    testBranch(offset, rs1 != rs2, (flag: Boolean) => flag)
  }
}

class BLT(c: Pc) extends Branch(c) {
  func = Func.BLT
  for (i <- 0 until 100) {
    val (offset, rs1, rs2) = genData(i)
    testBranch(offset, rs1 < rs2, (flag: Boolean) => flag)
  }
}

class BGE(c: Pc) extends Branch(c) {
  func = Func.BGE
  for (i <- 0 until 100) {
    val (offset, rs1, rs2) = genData(i)
    testBranch(offset, rs1 >= rs2, (flag: Boolean) => !flag)
  }
}

class BLTU(c: Pc) extends Branch(c) {
  func = Func.BLTU
  for (i <- 0 until 100) {
    val (offset, rs1, rs2) = genData(i)
    testBranch(offset,
         (rs1 | BigInt("0000000000000000", 16)) < (rs2 | BigInt("0000000000000000", 16)),
         (flag: Boolean) => flag)
  }
}

class BGEU(c: Pc) extends Branch(c) {
  func = Func.BGEU
  for (i <- 0 until 100) {
    val (offset, rs1, rs2) = genData(i)
    testBranch(offset,
         (rs1 | BigInt("0000000000000000", 16)) >= (rs2 | BigInt("0000000000000000", 16)),
         (flag: Boolean) => !flag)
  }
}
