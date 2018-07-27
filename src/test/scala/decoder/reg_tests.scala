package adept.decoder.tests.reg

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.decoder._
import adept.alu.AluOps
import adept.core.AdeptControlSignals

////////////////////////////////////////////////
// Test Suite for Register Type instructions
////////////////////////////////////////////////
class ADD(c: InstructionDecoder) extends DecoderTestBase(c) {
  private def ADD(rs1: Int, rs2: Int, imm: Int, rd: Int) {
    val instr = (((127 & imm) << 25) | ((31 & rs2) << 20) | 
                 ((31 & rs1) << 15) | ((31 & rd) << 7) | op_code.Registers.litValue())     
    val new_imm = if ((imm >> 6) == 1)
                    ((0xFFFFFFF << 7) | imm)
                  else 
                    imm
    val trap = if ((imm == 0) || (imm == funct7alu))
                 0
               else
                 1                    
    poke(c.io.stall_reg, false)
    poke(c.io.basic.instruction, instr)
    
    step(1)
    
    expect(c.io.basic.out.registers.we, true)
    expect(c.io.basic.out.registers.rsd_sel, rd)
    expect(c.io.basic.out.registers.rs1_sel, rs1)
    expect(c.io.basic.out.registers.rs2_sel, rs2)
    expect(c.io.basic.out.immediate, new_imm)
    if (imm == 0) {
      expect(c.io.basic.out.trap, trap)
      expect(c.io.basic.out.alu.op, AluOps.add)    
    } else if (imm == funct7alu) {
      expect(c.io.basic.out.trap, trap)
    } else {
      expect(c.io.basic.out.trap, trap)
    }
    expect(c.io.basic.out.sel_rf_wb, AdeptControlSignals.result_alu)
    expect(c.io.basic.out.sel_operand_a, AdeptControlSignals.sel_oper_A_rs1)
    expect(c.io.basic.out.sel_operand_b, AdeptControlSignals.sel_oper_B_rs2)
  }

  for (i <- 0 until 100) {
    var rs1 = rnd.nextInt(32)
    var rs2 = rnd.nextInt(32)
    val random = rnd.nextInt(128)
    val imm = if (random % 2 == 0)
                0
              else if (random % 5 == 0)
                funct7alu
              else
                random
    val rd  = rnd.nextInt(32)
    
    ADD(rs1, rs2, imm, rd)
  }
}
