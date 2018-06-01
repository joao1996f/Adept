package adept.pc.tests

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.config.AdeptConfig
import adept.pc.Pc

class BranchBase(c: Pc) extends PeekPokeTester(c) {
  val opcode = Integer.parseInt("1100011", 2)
  val pc_base = BigInt(0x10000000)

  // Branch Functions
  val BR_EQ_FUNC  = Integer.parseInt("000", 2)
  val BR_NE_FUNC  = Integer.parseInt("001", 2)
  val BR_LT_FUNC  = Integer.parseInt("100", 2)
  val BR_LTU_FUNC = Integer.parseInt("110", 2)
  val BR_GE_FUNC  = Integer.parseInt("101", 2)
  val BR_GEU_FUNC = Integer.parseInt("111", 2)

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
      case BR_NE_FUNC | BR_LT_FUNC | BR_LTU_FUNC if flags => return pc + offset
      case BR_EQ_FUNC | BR_GE_FUNC | BR_GEU_FUNC if !flags =>  return pc + offset
      case _ => return pc + 4
    }
  }

  def getSignExtend(imm: Int) : Int = {
    val bitMask = 0x1000

    val signExtend = if (((imm & bitMask) >>> 11) == 1) {
      0xFFFFE000
    } else {
      0x00000000
    }

    return signExtend
  }
}

class BR_EQ(c: Pc) extends BranchBase(c) {
  val max_offset = 0x1FFF
  var my_pc = pc_base

  private def BR_EQ(offset: Int, rs1: BigInt, rs2: BigInt) = {
    val flags = rs1 == rs2

    setBranchSignals(!flags, BR_EQ_FUNC, offset, my_pc)
    my_pc = evalBranch(!flags, BR_EQ_FUNC, offset, my_pc)

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

    BR_EQ(offset, rs1, rs2)
  }
}

class BR_NE(c: Pc) extends BranchBase(c) {
  val max_offset = 0x1FFF
  var my_pc = pc_base

  private def BR_NE(offset: Int, rs1: BigInt, rs2: BigInt) = {
    val flags = rs1 != rs2

    setBranchSignals(flags, BR_NE_FUNC, offset, my_pc)
    my_pc = evalBranch(flags, BR_NE_FUNC, offset, my_pc)

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

    BR_NE(offset, rs1, rs2)
  }
}

class BR_LT(c: Pc) extends BranchBase(c) {
  val max_offset = 0x1FFF
  var my_pc = pc_base

  private def BR_LT(offset: Int, rs1: BigInt, rs2: BigInt) = {
    val flags = rs1 < rs2

    setBranchSignals(flags, BR_LT_FUNC, offset, my_pc)
    my_pc = evalBranch(flags, BR_LT_FUNC, offset, my_pc)

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

    BR_LT(offset, rs1, rs2)
  }
}

class BR_GE(c: Pc) extends BranchBase(c) {
  val max_offset = 0x1FFF
  var my_pc = pc_base

  private def BR_GE(offset: Int, rs1: BigInt, rs2: BigInt) = {
    val flags = rs1 < rs2

    setBranchSignals(flags, BR_GE_FUNC, offset, my_pc)
    my_pc = evalBranch(flags, BR_GE_FUNC, offset, my_pc)

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

    BR_GE(offset, rs1, rs2)
  }
}

class BR_LTU(c: Pc) extends BranchBase(c) {
  val max_offset = 0x1FFF
  var my_pc = pc_base

  private def BR_LTU(offset: Int, rs1: BigInt, rs2: BigInt) = {
    val flags = (rs1 | BigInt("0000000000000000", 16)) < (rs2 | BigInt("0000000000000000", 16))

    setBranchSignals(flags, BR_LTU_FUNC, offset, my_pc)
    my_pc = evalBranch(flags, BR_LTU_FUNC, offset, my_pc)

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

    BR_LTU(offset, rs1, rs2)
  }
}
