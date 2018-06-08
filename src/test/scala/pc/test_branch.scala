package adept.pc.tests

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.config.AdeptConfig
import adept.pc.Pc

class BranchBase(c: Pc) extends PeekPokeTester(c) {
  val opcode = Integer.parseInt("1100011", 2)
  val pc_base = BigInt(0x10000000)

  // Branch Functions
  object Func extends Enumeration {
    val BEQ  = Value(Integer.parseInt("000", 2))
    val BNE  = Value(Integer.parseInt("001", 2))
    val BLT  = Value(Integer.parseInt("100", 2))
    val BLTU = Value(Integer.parseInt("110", 2))
    val BGE  = Value(Integer.parseInt("101", 2))
    val BGEU = Value(Integer.parseInt("111", 2))
    val Empty = Value(Integer.parseInt("010", 2))
  }
  import Func._

  val max_offset = 0x1FFF
  var my_pc = pc_base
  var func = Func.Empty

  def setBranchSignals(flags: Boolean, offset: Int) = {
    // Decoder result
    poke(c.io.in_opcode, opcode)
    poke(c.io.br_func, func.id)
    // PC offset for JAL or Branch
    poke(c.io.br_offset, offset)

    // ALU comparison result
    poke(c.io.br_flags, flags)

    // Jump address for JALR, this is a don't care in this section
    poke(c.io.br_step, 0)
    // PC delayed one clock cycle
    poke(c.io.pc_in, my_pc)

    // TODO: Don't ignore stalls
    poke(c.io.stall, 0)
    poke(c.io.mem_en, 0)
  }

  def expectBranchSignals(stall: Boolean) = {
    expect(c.io.pc_out, my_pc)
    expect(c.io.stall_reg, stall)
  }

  def evalBranch(flag: Boolean, offset: Int) : BigInt = {
    func match {
      case BNE | BLT | BLTU if flag => return my_pc + offset
      case BEQ | BGE | BGEU if !flag =>  return my_pc + offset
      case _ => return my_pc + 4
    }
  }

  def getSignExtend(imm: Int) : Int = {
    val bitMask = 0x1000

    val signExtend = if (((imm & bitMask) >>> 12) == 1) {
      0xFFFFE000
    } else {
      0x00000000
    }

    return signExtend
  }

  def genData(i: Int) : (Int, BigInt, BigInt) = {
    val rs1 = BigInt(32, rnd)
    val rs2 = if (i % 2 == 0) {
      BigInt(32, rnd)
    } else {
      rs1
    }

    val imm = rnd.nextInt(max_offset) & (max_offset - 1)
    val offset = imm | getSignExtend(imm)

    return (offset, rs1, rs2)
  }

  def branchHazardStall(n_cycles: Int, flag: Boolean) = {
    if (flag) {
      // Because I'm forcing the branch to be taken,
      // I need to insert a non control instruction opcode
      // in the next instruction
      poke(c.io.in_opcode, 0)

      step(n_cycles)
    }
  }

  def testBranch(offset: Int, flag: Boolean, hw_flag: (Boolean) => Boolean) = {
    setBranchSignals(hw_flag(flag), offset)
    my_pc = evalBranch(hw_flag(flag), offset)

    step(1)
    branchHazardStall(1, flag)

    expectBranchSignals(false)
  }
}

class BEQ(c: Pc) extends BranchBase(c) {
  func = Func.BEQ
  for (i <- 0 until 100) {
    val (offset, rs1, rs2) = genData(i)
    testBranch(offset, rs1 == rs2, (flag: Boolean) => !flag)
  }
}

class BNE(c: Pc) extends BranchBase(c) {
  func = Func.BNE
  for (i <- 0 until 100) {
    val (offset, rs1, rs2) = genData(i)
    testBranch(offset, rs1 != rs2, (flag: Boolean) => flag)
  }
}

class BLT(c: Pc) extends BranchBase(c) {
  func = Func.BLT
  for (i <- 0 until 100) {
    val (offset, rs1, rs2) = genData(i)
    testBranch(offset, rs1 < rs2, (flag: Boolean) => flag)
  }
}

class BGE(c: Pc) extends BranchBase(c) {
  func = Func.BGE
  for (i <- 0 until 100) {
    val (offset, rs1, rs2) = genData(i)
    testBranch(offset, rs1 >= rs2, (flag: Boolean) => !flag)
  }
}

class BLTU(c: Pc) extends BranchBase(c) {
  func = Func.BLTU
  for (i <- 0 until 100) {
    val (offset, rs1, rs2) = genData(i)
    testBranch(offset,
         (rs1 | BigInt("0000000000000000", 16)) < (rs2 | BigInt("0000000000000000", 16)),
         (flag: Boolean) => flag)
  }
}

class BGEU(c: Pc) extends BranchBase(c) {
  func = Func.BGEU
  for (i <- 0 until 100) {
    val (offset, rs1, rs2) = genData(i)
    testBranch(offset,
         (rs1 | BigInt("0000000000000000", 16)) >= (rs2 | BigInt("0000000000000000", 16)),
         (flag: Boolean) => !flag)
  }
}
