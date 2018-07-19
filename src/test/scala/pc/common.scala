package adept.pc.tests

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.config.AdeptConfig
import adept.pc.Pc
import adept.pc.PcOps

class ControlCommon(c: Pc) extends PeekPokeTester(c) {
  var my_pc = BigInt("10000000", 16)
  val pc_ops = PcOps

  def evalBranch(op: BigInt, offset: BigInt, rs1: BigInt = 0, flag: Boolean = false) : BigInt = {
    if ((op == pc_ops.bne.litValue() || op == pc_ops.blt.litValue() ||
           op == pc_ops.bltu.litValue()) && flag) {
      return my_pc + offset
    } else if ((op == pc_ops.beq.litValue() || op == pc_ops.bge.litValue() ||
                  op == pc_ops.bgeu.litValue()) && !flag) {
      return my_pc + offset
    } else if (op == pc_ops.jal.litValue()) {
      return my_pc + offset
    } else if (op == pc_ops.jalr.litValue()) {
      return (offset + rs1) & BigInt("00FFFFFFFE", 16)
    } else {
      return my_pc + 4
    }
  }

  def setBranchSignals(op: BigInt, offset: BigInt, flag: Boolean = false, rs1: BigInt = 0) = {
    // Decoder result
    poke(c.io.decoder.op, op)
    poke(c.io.decoder.br_offset, offset)

    // ALU comparison result
    poke(c.io.flag, flag)

    // RS1 Value
    poke(c.io.rs1, rs1)
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
      // Because I'm forcing the branch to be taken, I need to insert a non
      // control op in the next instruction
      poke(c.io.decoder.op, pc_ops.no_jmp)

      step(n_cycles)
    }
  }
}
