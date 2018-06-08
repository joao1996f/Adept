package adept.pc.tests

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.config.AdeptConfig
import adept.pc.Pc

class BranchBase(c: Pc) extends PeekPokeTester(c) {
  val opcode = Integer.parseInt("1100011", 2)
  val pc_base = BigInt(0x10000000)

  // Branch Functions
  val BEQ_FUNC  = Integer.parseInt("000", 2)
  val BNE_FUNC  = Integer.parseInt("001", 2)
  val BLT_FUNC  = Integer.parseInt("100", 2)
  val BLTU_FUNC = Integer.parseInt("110", 2)
  val BGE_FUNC  = Integer.parseInt("101", 2)
  val BGEU_FUNC = Integer.parseInt("111", 2)

  def setBranchSignals(flags: Boolean, func: Int, offset: Int, pc_in: BigInt) = {
    // Decoder result
    poke(c.io.in_opcode, opcode)
    poke(c.io.br_func, func)
    // PC offset for JAL or Branch
    poke(c.io.br_offset, offset)

    // ALU comparison result
    poke(c.io.br_flags, flags)

    // Jump address for JALR, this is a don't care in this section
    poke(c.io.br_step, 0)
    // PC delayed one clock cycle
    poke(c.io.pc_in, pc_in)

    // TODO: Don't ignore stalls
    poke(c.io.stall, 0)
    poke(c.io.mem_en, 0)
  }

  def expectBranchSignals(my_pc: BigInt, stall: Boolean) = {
    expect(c.io.pc_out, my_pc)
    expect(c.io.stall_reg, stall)
  }

  def evalBranch(flags: Boolean, func: Int, offset: Int, pc: BigInt) : BigInt = {
    func match {
      case BNE_FUNC | BLT_FUNC | BLTU_FUNC if flags => return pc + offset
      case BEQ_FUNC | BGE_FUNC | BGEU_FUNC if !flags =>  return pc + offset
      case _ => return pc + 4
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

}

class BEQ(c: Pc) extends BranchBase(c) {
  val max_offset = 0x1FFF
  var my_pc = pc_base

  private def BEQ(offset: Int, rs1: BigInt, rs2: BigInt) = {
    val flags = rs1 == rs2

    setBranchSignals(!flags, BEQ_FUNC, offset, my_pc)
    my_pc = evalBranch(!flags, BEQ_FUNC, offset, my_pc)

    step(1)

    if (flags) {
      // Because I'm forcing the branch to be taken,
      // I need to insert a non control instruction opcode
      // in the next instruction
      poke(c.io.in_opcode, 0)

      step(1)
    }

    expectBranchSignals(my_pc, false)
  }

  for (i <- 0 until 100) {
    val rs1 = BigInt(32, rnd)
    val rs2 = if (i % 2 == 0) {
      BigInt(32, rnd)
    } else {
      rs1
    }

    val imm = rnd.nextInt(max_offset) & 0x1FFE
    val offset = imm | getSignExtend(imm)

    BEQ(offset, rs1, rs2)
  }
}

class BNE(c: Pc) extends BranchBase(c) {
  val max_offset = 0x1FFF
  var my_pc = pc_base

  private def BNE(offset: Int, rs1: BigInt, rs2: BigInt) = {
    val flags = rs1 != rs2

    setBranchSignals(flags, BNE_FUNC, offset, my_pc)
    my_pc = evalBranch(flags, BNE_FUNC, offset, my_pc)

    step(1)

    if (flags) {
      // Because I'm forcing the branch to be taken,
      // I need to insert a non control instruction opcode
      // in the next instruction
      poke(c.io.in_opcode, 0)

      step(1)
    }

    expectBranchSignals(my_pc, false)
  }

  for (i <- 0 until 100) {
    val rs1 = BigInt(32, rnd)
    val rs2 = if (i % 2 == 0) {
      BigInt(32, rnd)
    } else {
      rs1
    }

    val imm = rnd.nextInt(max_offset) & 0x1FFE
    val offset = imm | getSignExtend(imm)

    BNE(offset, rs1, rs2)
  }
}

class BLT(c: Pc) extends BranchBase(c) {
  val max_offset = 0x1FFF
  var my_pc = pc_base

  private def BLT(offset: Int, rs1: BigInt, rs2: BigInt) = {
    val flags = rs1 < rs2

    setBranchSignals(flags, BLT_FUNC, offset, my_pc)
    my_pc = evalBranch(flags, BLT_FUNC, offset, my_pc)

    step(1)

    if (flags) {
      // Because I'm forcing the branch to be taken,
      // I need to insert a non control instruction opcode
      // in the next instruction
      poke(c.io.in_opcode, 0)

      step(1)
    }

    expectBranchSignals(my_pc, false)
  }

  for (i <- 0 until 100) {
    val rs1 = BigInt(32, rnd)
    val rs2 = if (i % 2 == 0) {
      BigInt(32, rnd)
    } else {
      rs1
    }

    val imm = rnd.nextInt(max_offset) & (max_offset - 1)
    val offset = imm | getSignExtend(imm)

    BLT(offset, rs1, rs2)
  }
}

class BGE(c: Pc) extends BranchBase(c) {
  val max_offset = 0x1FFF
  var my_pc = pc_base

  private def BGE(offset: Int, rs1: BigInt, rs2: BigInt) = {
    val flags = rs1 < rs2

    setBranchSignals(flags, BGE_FUNC, offset, my_pc)
    my_pc = evalBranch(flags, BGE_FUNC, offset, my_pc)

    step(1)

    if (!flags) {
      // Because I'm forcing the branch to be taken,
      // I need to insert a non control instruction opcode
      // in the next instruction
      poke(c.io.in_opcode, 0)

      step(1)
    }

    expectBranchSignals(my_pc, false)
  }

  for (i <- 0 until 100) {
    val rs1 = BigInt(32, rnd)
    val rs2 = if (i % 2 == 0) {
      BigInt(32, rnd)
    } else {
      rs1
    }

    val imm = rnd.nextInt(max_offset) & 0x1FFE
    val offset = imm | getSignExtend(imm)

    BGE(offset, rs1, rs2)
  }
}

class BLTU(c: Pc) extends BranchBase(c) {
  val max_offset = 0x1FFF
  var my_pc = pc_base

  private def BLTU(offset: Int, rs1: BigInt, rs2: BigInt) = {
    val flags = (rs1 | BigInt("0000000000000000", 16)) < (rs2 | BigInt("0000000000000000", 16))

    setBranchSignals(flags, BLTU_FUNC, offset, my_pc)
    my_pc = evalBranch(flags, BLTU_FUNC, offset, my_pc)

    step(1)

    if (flags) {
      // Because I'm forcing the branch to be taken,
      // I need to insert a non control instruction opcode
      // in the next instruction
      poke(c.io.in_opcode, 0)

      step(1)
    }

    expectBranchSignals(my_pc, false)
  }

  for (i <- 0 until 100) {
    val rs1 = BigInt(32, rnd)
    val rs2 = if (i % 2 == 0) {
      BigInt(32, rnd)
    } else {
      rs1
    }

    val imm = rnd.nextInt(max_offset) & 0x1FFE
    val offset = imm | getSignExtend(imm)

    BLTU(offset, rs1, rs2)
  }
}

class BGEU(c: Pc) extends BranchBase(c) {
  val max_offset = 0x1FFF
  var my_pc = pc_base

  private def BGEU(offset: Int, rs1: BigInt, rs2: BigInt) = {
    val flags = (rs1 | BigInt("0000000000000000", 16)) < (rs2 | BigInt("0000000000000000", 16))

    setBranchSignals(flags, BGEU_FUNC, offset, my_pc)
    my_pc = evalBranch(flags, BGEU_FUNC, offset, my_pc)

    step(1)

    if (!flags) {
      // Because I'm forcing the branch to be taken,
      // I need to insert a non control instruction opcode
      // in the next instruction
      poke(c.io.in_opcode, 0)

      step(1)
    }

    expectBranchSignals(my_pc, false)
  }

  for (i <- 0 until 100) {
    val rs1 = BigInt(32, rnd)
    val rs2 = if (i % 2 == 0) {
      BigInt(32, rnd)
    } else {
      rs1
    }

    val imm = rnd.nextInt(max_offset) & 0x1FFE
    val offset = imm | getSignExtend(imm)

    BGEU(offset, rs1, rs2)
  }
}
