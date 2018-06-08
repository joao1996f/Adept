package adept.pc.tests

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.config.AdeptConfig
import adept.pc.Pc

class Jump(c: Pc) extends PeekPokeTester(c) {
  val pc_base = BigInt(0x10000000)
  var my_pc = pc_base

  def setBranchSignals(opcode:Int, offset: Int, pc_in: BigInt) = {
    // Decoder result
    poke(c.io.in_opcode, opcode)
    poke(c.io.br_func, 0)
    // PC offset for JAL or Branch
    poke(c.io.br_offset, offset)

    // ALU comparison result
    poke(c.io.br_flags, 0) // Don't care

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

  def evalBranch(offset: Int, pc: BigInt) : BigInt = {
    return pc + offset
  }

  def getSignExtend(imm: Int, bitMask: Int) : Int = {
    if ((imm >>> 20) == 1) {
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
    setBranchSignals(opcode, offset, my_pc)
    my_pc = evalBranch(offset, my_pc)

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
