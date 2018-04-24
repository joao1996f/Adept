// ALU Unit Tester
package adept.alu

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.config.AdeptConfig

/*
 * ALU Unit Tester
 *
 * The unit tester will run through all ALU pertinent instructions to check for
 * their correct functionality. Each test should note what each input is doing,
 * and why it's important, followed by its expected output. In the case of an
 * edge case test, the author should provide a detailed explanation of why their
 * set of inputs fails to achieve the correct result, and why their fix works.
 * If need be, please quote the ISA specification.
 */
class ALUUnitTester(c: ALU) extends PeekPokeTester(c) {
  ////////////////////////////////////////////////
  // Immediate Type instructions
  ////////////////////////////////////////////////

  // ADDI
  private def ADDI(rs1: Int, imm: Int) {
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.decoder_params.imm, imm)
    poke(c.io.in.decoder_params.switch_2_imm, true)
    poke(c.io.in.decoder_params.op, 0) // b000
    poke(c.io.in.decoder_params.op_code, 19) // b0010011
    step(1)
    expect(c.io.result, rs1 + imm)
  }

  // SLLI
  private def SLLI(rs1: Int, imm: Int) {
    val special_imm = 31 & imm // h_0000_001f
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.decoder_params.switch_2_imm, true)
    poke(c.io.in.decoder_params.imm, special_imm)
    poke(c.io.in.decoder_params.op, 1) // b001
    poke(c.io.in.decoder_params.op_code, 19) // b0010011
    step(1)
    expect(c.io.result, rs1 << special_imm)
  }

  // SLTI
  private def SLTI(rs1: Int, imm: Int) {
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.decoder_params.switch_2_imm, true)
    poke(c.io.in.decoder_params.imm, imm)
    poke(c.io.in.decoder_params.op, 2) // b010
    poke(c.io.in.decoder_params.op_code, 19) // b0010011
    step(1)
    expect(c.io.result, rs1 < imm)
  }

  // SLTIU
  private def SLTIU(rs1: Int, imm: Int) {
    // Turns out Scala doesn't have unsigned types so we do this trickery
    val u_rs1 = rs1.asInstanceOf[Long] & 0x00000000ffffffffL
    val u_imm = imm.asInstanceOf[Long] & 0x00000000ffffffffL
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.decoder_params.switch_2_imm, true)
    poke(c.io.in.decoder_params.imm, imm)
    poke(c.io.in.decoder_params.op, 3) // b011
    poke(c.io.in.decoder_params.op_code, 19) // b0010011
    step(1)
    expect(c.io.result, u_rs1 < u_imm)
  }

  // XORI
  private def XORI(rs1: Int, imm: Int) {
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.decoder_params.imm, imm)
    poke(c.io.in.decoder_params.op, 4) // b100
    poke(c.io.in.decoder_params.op_code, 19) // b0010011
    step(1)
    expect(c.io.result, rs1 ^ imm)
  }

  // SRLI
  private def SRLI(rs1: Int, imm: Int) {
    val special_imm =  31 & imm // h_0000_001f
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.decoder_params.switch_2_imm, true)
    poke(c.io.in.decoder_params.imm, special_imm)
    poke(c.io.in.decoder_params.op, 5) // b101
    poke(c.io.in.decoder_params.op_code, 19) // b0010011
    step(1)
    // >>> is the logic right shift operator
    expect(c.io.result, rs1 >>> special_imm)
  }

  // SRAI
  private def SRAI(rs1: Int, imm: Int) {
    val special_imm_2_shift =  31 & imm // h_0000_001f
    val special_imm =  1024 | special_imm_2_shift // h_0000_0400
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.decoder_params.switch_2_imm, true)
    poke(c.io.in.decoder_params.imm, special_imm)
    poke(c.io.in.decoder_params.op, 5) // b101
    poke(c.io.in.decoder_params.op_code, 19) // b0010011
    step(1)
    expect(c.io.result, rs1 >> special_imm_2_shift)
  }

  // ORI
  private def ORI(rs1: Int, imm: Int) {
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.decoder_params.switch_2_imm, true)
    poke(c.io.in.decoder_params.imm, imm)
    poke(c.io.in.decoder_params.op, 6) // b110
    poke(c.io.in.decoder_params.op_code, 19) // b0010011
    step(1)
    expect(c.io.result, rs1 | imm)
  }

  // ANDI
  private def ANDI(rs1: Int, imm: Int) {
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.decoder_params.switch_2_imm, true)
    poke(c.io.in.decoder_params.imm, imm)
    poke(c.io.in.decoder_params.op, 7) // b111
    poke(c.io.in.decoder_params.op_code, 19) // b0010011
    step(1)
    expect(c.io.result, rs1 & imm)
  }

  ////////////////////////////////////////////////
  // Register Type instructions
  ////////////////////////////////////////////////

  // ADD
  private def ADD(rs1: Int, rs2: Int, imm: Int) {
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.registers.rs2, rs2)
    poke(c.io.in.decoder_params.switch_2_imm, false)
    poke(c.io.in.decoder_params.imm, 31 & imm)
    poke(c.io.in.decoder_params.op, 0) // b000
    poke(c.io.in.decoder_params.op_code, 51) // b0110011
    step(1)
    expect(c.io.result, rs1 + rs2)
  }

  // SUB
  private def SUB(rs1: Int, rs2: Int, imm: Int) {
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.registers.rs2, rs2)
    poke(c.io.in.decoder_params.switch_2_imm, false)
    poke(c.io.in.decoder_params.imm, 1024)
    poke(c.io.in.decoder_params.op, 0) // b000
    poke(c.io.in.decoder_params.op_code, 51) // b0110011
    step(1)
    expect(c.io.result, rs1 - rs2)
  }

  // SLL
  private def SLL(rs1: Int, rs2: Int, imm: Int) {
    val special_rs2 = 31 & rs2 // h_0000_001f
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.registers.rs2, special_rs2)
    poke(c.io.in.decoder_params.switch_2_imm, false)
    poke(c.io.in.decoder_params.imm, 31 & imm)
    poke(c.io.in.decoder_params.op, 1) // b001
    poke(c.io.in.decoder_params.op_code, 51) // b0110011
    step(1)
    expect(c.io.result, rs1 << special_rs2)
  }

  // SLT
  private def SLT(rs1: Int, rs2: Int, imm: Int) {
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.registers.rs2, rs2)
    poke(c.io.in.decoder_params.imm, 31 & imm)
    poke(c.io.in.decoder_params.switch_2_imm, false)
    poke(c.io.in.decoder_params.op, 2) // b010
    poke(c.io.in.decoder_params.op_code, 51) // b0110011
    step(1)
    expect(c.io.result, rs1 < rs2)
  }

  // SLTU
  private def SLTU(rs1: Int, rs2: Int, imm: Int) {
    // Turns out Scala doesn't have unsigned types so we do this trickery
    val u_rs1 = rs1.asInstanceOf[Long] & 0x00000000ffffffffL
    val u_rs2 = rs2.asInstanceOf[Long] & 0x00000000ffffffffL
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.registers.rs2, rs2)
    poke(c.io.in.decoder_params.imm, 31 & imm)
    poke(c.io.in.decoder_params.switch_2_imm, false)
    poke(c.io.in.decoder_params.op, 3) // b011
    poke(c.io.in.decoder_params.op_code, 51) // b0110011
    step(1)
    expect(c.io.result, u_rs1 < u_rs2)
  }

  // XOR
  private def XOR(rs1: Int, rs2: Int, imm: Int) {
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.registers.rs2, rs2)
    poke(c.io.in.decoder_params.imm, 31 & imm)
    poke(c.io.in.decoder_params.switch_2_imm, false)
    poke(c.io.in.decoder_params.op, 4) // b100
    poke(c.io.in.decoder_params.op_code, 51) // b0110011
    step(1)
    expect(c.io.result, rs1 ^ rs2)
  }

  // SRL
  private def SRL(rs1: Int, rs2: Int, imm: Int) {
    val special_rs2 =  31 & rs2 // h_0000_001f
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.registers.rs2, special_rs2)
    poke(c.io.in.decoder_params.imm, 31 & imm)
    poke(c.io.in.decoder_params.switch_2_imm, false)
    poke(c.io.in.decoder_params.op, 5) // b101
    poke(c.io.in.decoder_params.op_code, 51) // b0110011
    step(1)
    // >>> is the logic right shift operator
    expect(c.io.result, rs1 >>> special_rs2)
  }

  // SRA
  private def SRA(rs1: Int, rs2: Int, imm: Int) {
    val special_rs2_2_shift =  31 & rs2 // h_0000_001f
    val special_rs2 =  1024 | special_rs2_2_shift // h_0000_0400
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.registers.rs2, special_rs2)
    poke(c.io.in.decoder_params.switch_2_imm, false)
    poke(c.io.in.decoder_params.imm, 1024)
    poke(c.io.in.decoder_params.op, 5) // b101
    poke(c.io.in.decoder_params.op_code, 51) // b0110011
    step(1)
    expect(c.io.result, rs1 >> special_rs2_2_shift)
  }

  // OR
  private def OR(rs1: Int, rs2: Int, imm: Int) {
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.registers.rs2, rs2)
    poke(c.io.in.decoder_params.imm, 31 & imm)
    poke(c.io.in.decoder_params.switch_2_imm, false)
    poke(c.io.in.decoder_params.op, 6) // b110
    poke(c.io.in.decoder_params.op_code, 51) // b0110011
    step(1)
    expect(c.io.result, rs1 | rs2)
  }

  // AND
  private def AND(rs1: Int, rs2: Int, imm: Int) {
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.registers.rs2, rs2)
    poke(c.io.in.decoder_params.imm, 31 & imm)
    poke(c.io.in.decoder_params.switch_2_imm, false)
    poke(c.io.in.decoder_params.op, 7) // b111
    poke(c.io.in.decoder_params.op_code, 51) // b0110011
    step(1)
    expect(c.io.result, rs1 & rs2)
  }

  // Fuzz here
  // Generate a random rs1 value and a random immediate
  for (i <- 0 until 10000) {
    var rs1 = rnd.nextInt(2000000000)
    var rs2 = rnd.nextInt(2000000000)
    var imm = rnd.nextInt(2000000000)

    val signedness = rnd.nextInt(50)
    if (signedness % 4 == 0) {
      rs1 = rs1 * -1
      imm = imm * -1
      rs2 = rs2 * -1
    } else if (signedness % 5 == 0) {
      rs2 = rs2 * -1
    } else if (signedness % 2 == 0) {
      rs1 = rs1 * -1
    } else if (signedness % 3 == 0) {
      imm = imm * -1
    }

    // Immediate Type instructions
    ADDI(rs1, imm)
    XORI(rs1, imm)
    ORI(rs1, imm)
    ANDI(rs1, imm)
    SLLI(rs1, imm)
    SRAI(rs1, imm)
    SRLI(rs1, imm)
    SLTI(rs1, imm)
    SLTIU(rs1, imm)

    // Register Type instructions
    ADD(rs1, rs2, imm)
    SUB(rs1, rs2, imm)
    XOR(rs1, rs2, imm)
    OR(rs1, rs2, imm)
    AND(rs1, rs2, imm)
    SLL(rs1, rs2, imm)
    SRA(rs1, rs2, imm)
    SRL(rs1, rs2, imm)
    SLT(rs1, rs2, imm)
    SLTU(rs1, rs2, imm)
  }
}

class ALUTester extends ChiselFlatSpec {
  // Generate configuration
  val config = new AdeptConfig

  private val backendNames = Array("firrtl", "verilator")

  for ( backendName <- backendNames ) {
    "ALU" should s"do stuff (with $backendName)" in {
      Driver(() => new ALU(config), backendName) {
        e => new ALUUnitTester(e)
      } should be (true)
    }
  }
}
