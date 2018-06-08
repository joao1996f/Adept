package adept.pc.tests

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.config.AdeptConfig
import adept.pc.Pc

class ControlCommon(c: Pc) extends PeekPokeTester(c) {
  val pc_base = BigInt(0x10000000)
  var my_pc = pc_base

  def setBranchSignals(opcode: Int, offset: BigInt, func: Int = 0, flag: Boolean = false) = {
    // Decoder result
    poke(c.io.in_opcode, opcode)
    poke(c.io.br_func, func)
    // PC offset for JAL or Branch
    poke(c.io.br_offset, offset)

    // ALU comparison result
    poke(c.io.br_flags, flag)

    // Jump address for JALR
    poke(c.io.br_step, offset)
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

  def getSignExtend(imm: Int, bitMask: Int) : Int = {
    val shift = (scala.math.log(bitMask) / scala.math.log(2)).toInt

    if ((imm >>> shift) == 1) {
      return (0xFFFFFFFF ^ bitMask) & ~(bitMask - 1)
    } else {
      return 0x00000000
    }
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
}
