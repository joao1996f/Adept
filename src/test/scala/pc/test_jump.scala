package adept.pc.tests

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.config.AdeptConfig
import adept.pc.Pc

class Jump(c: Pc) extends PeekPokeTester(c) {
  val pc_base = BigInt(0x10000000)
  var my_pc = pc_base

  object Op extends Enumeration {
    type Op = Value
    val JAL, JALR = Value
  }
  import Op._

  def setBranchSignals(opcode: Int, offset: BigInt, pc_in: BigInt, func: Int) = {
    // Decoder result
    poke(c.io.in_opcode, opcode)
    poke(c.io.br_func, func)
    // PC offset for JAL or Branch
    poke(c.io.br_offset, offset)

    // ALU comparison result
    poke(c.io.br_flags, 0) // Don't care

    // Jump address for JALR
    poke(c.io.br_step, offset)
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

  def evalBranch(op: Op, offset: Int, pc: BigInt, rs1: BigInt) : BigInt = {
    op match {
      case JAL => return pc + offset
      case JALR => return (rs1 + offset) & 0xFFFFFFFE
    }
  }

  def getSignExtend(imm: Int, bitMask: Int) : Int = {
    val shift = (scala.math.log(bitMask) / scala.math.log(2)).toInt

    if ((imm >>> shift) == 1) {
      return (0xFFFFFFFF ^ bitMask) & ~(bitMask - 1)
    } else {
      return 0x00000000
    }
  }
}

class JAL(c: Pc) extends Jump(c) {
  val max_offset = 0x1FFFFF
  val opcode = Integer.parseInt("1101111", 2)

  private def JAL(offset: Int) = {
    setBranchSignals(opcode, offset, my_pc, 0)
    my_pc = evalBranch(Op.JAL, offset, my_pc, 0)

    step(1)

    poke(c.io.in_opcode, 0)

    step(1)

    expectBranchSignals(my_pc, false)
  }

  for (i <- 0 until 100) {
    val imm = rnd.nextInt(max_offset) & (max_offset - 1)
    val offset = imm | getSignExtend(imm, 0x100000)

    JAL(offset)
  }
}

class JALR(c: Pc) extends Jump(c) {
  val max_offset = 0xFFF
  val opcode = Integer.parseInt("1100111", 2)
  val func = Integer.parseInt("000", 2)

  private def JALR(offset: Int, rs1: BigInt) = {
    setBranchSignals(opcode, rs1 + offset, my_pc, func)
    my_pc = evalBranch(Op.JALR, offset, my_pc, rs1)

    step(1)

    poke(c.io.in_opcode, 0)

    step(1)

    expectBranchSignals(my_pc, false)
  }

  for (i <- 0 until 100) {
    val rs1 = BigInt(32, rnd)
    val imm = rnd.nextInt(max_offset)
    val offset = imm | getSignExtend(imm, 0x800)

    JALR(offset, rs1)
  }
}
