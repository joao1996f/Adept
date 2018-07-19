package adept.pc.tests

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.config.AdeptConfig
import adept.pc.Pc

class Branch(c: Pc) extends ControlCommon(c) {
  val opcode = Integer.parseInt("1100011", 2)
  val max_offset = 0x1FFF
  val bitMask = 0x1000

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

  def testBranch(op: BigInt, offset: Int, flag: Boolean, hw_flag: (Boolean) => Boolean) = {
    setBranchSignals(op, offset, hw_flag(flag))
    my_pc = evalBranch(op, offset, flag = hw_flag(flag))

    step(1)
    branchHazardStall(1, flag)

    expectBranchSignals(false)
  }
}

class BEQ(c: Pc) extends Branch(c) {
  for (i <- 0 until 100) {
    val (offset, rs1, rs2) = genData(i)
    testBranch(pc_ops.beq.litValue(), offset, rs1 == rs2, (flag: Boolean) => !flag)
  }
}

class BNE(c: Pc) extends Branch(c) {
  for (i <- 0 until 100) {
    val (offset, rs1, rs2) = genData(i)
    testBranch(pc_ops.bne.litValue(), offset, rs1 != rs2, (flag: Boolean) => flag)
  }
}

class BLT(c: Pc) extends Branch(c) {
  for (i <- 0 until 100) {
    val (offset, rs1, rs2) = genData(i)
    testBranch(pc_ops.blt.litValue(), offset, rs1 < rs2, (flag: Boolean) => flag)
  }
}

class BGE(c: Pc) extends Branch(c) {
  for (i <- 0 until 100) {
    val (offset, rs1, rs2) = genData(i)
    testBranch(pc_ops.bge.litValue(), offset, rs1 >= rs2, (flag: Boolean) => !flag)
  }
}

class BLTU(c: Pc) extends Branch(c) {
  for (i <- 0 until 100) {
    val (offset, rs1, rs2) = genData(i)
    testBranch(pc_ops.bltu.litValue(), offset,
         (rs1 | BigInt("0000000000000000", 16)) < (rs2 | BigInt("0000000000000000", 16)),
         (flag: Boolean) => flag)
  }
}

class BGEU(c: Pc) extends Branch(c) {
  for (i <- 0 until 100) {
    val (offset, rs1, rs2) = genData(i)
    testBranch(pc_ops.bgeu.litValue(), offset,
         (rs1 | BigInt("0000000000000000", 16)) >= (rs2 | BigInt("0000000000000000", 16)),
         (flag: Boolean) => !flag)
  }
}
