package adept.alu.imm

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.alu._
import adept.idecode.OpCodes

////////////////////////////////////////////////
// Test Suite for Immediate Type instructions
////////////////////////////////////////////////
class ADDI(c: ALU) extends ALUTestBase(c) {
  private def ADDI(rs1: Int, imm: Int) {
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.decoder_params.imm, imm)
    poke(c.io.in.decoder_params.switch_2_imm, true)
    poke(c.io.in.decoder_params.op, alu_ops.add)
    poke(c.io.in.decoder_params.op_code, op_code.Immediate)
    step(1)
    expect(c.io.result, rs1 + imm)
  }

  for (i <- 0 until 100) {
    var rs1 = rnd.nextInt(2000000000)
    var imm = rnd.nextInt(2000000000)

    val signedness = rnd.nextInt(50)
    if (signedness % 4 == 0) {
      rs1 = rs1 * -1
      imm = imm * -1
    } else if (signedness % 2 == 0) {
      rs1 = rs1 * -1
    } else if (signedness % 3 == 0) {
      imm = imm * -1
    }

    ADDI(rs1, imm)
  }
}

class SLLI(c: ALU) extends ALUTestBase(c) {
  private def SLLI(rs1: Int, imm: Int) {
    val special_imm = 31 & imm // h_0000_001f
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.decoder_params.switch_2_imm, true)
    poke(c.io.in.decoder_params.imm, special_imm)
    poke(c.io.in.decoder_params.op, alu_ops.sll)
    poke(c.io.in.decoder_params.op_code, op_code.Immediate)
    step(1)
    expect(c.io.result, rs1 << special_imm)
  }

  for (i <- 0 until 100) {
    var rs1 = rnd.nextInt(2000000000)
    var imm = rnd.nextInt(2000000000)

    val signedness = rnd.nextInt(50)
    if (signedness % 4 == 0) {
      rs1 = rs1 * -1
      imm = imm * -1
    } else if (signedness % 2 == 0) {
      rs1 = rs1 * -1
    } else if (signedness % 3 == 0) {
      imm = imm * -1
    }

    SLLI(rs1, imm)
  }
}

class SLTI(c: ALU) extends ALUTestBase(c) {
  private def SLTI(rs1: Int, imm: Int) {
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.decoder_params.switch_2_imm, true)
    poke(c.io.in.decoder_params.imm, imm)
    poke(c.io.in.decoder_params.op, alu_ops.slt)
    poke(c.io.in.decoder_params.op_code, op_code.Immediate)
    step(1)
    expect(c.io.result, rs1 < imm)
  }

  for (i <- 0 until 100) {
    var rs1 = rnd.nextInt(2000000000)
    var imm = rnd.nextInt(2000000000)

    val signedness = rnd.nextInt(50)
    if (signedness % 4 == 0) {
      rs1 = rs1 * -1
      imm = imm * -1
    } else if (signedness % 2 == 0) {
      rs1 = rs1 * -1
    } else if (signedness % 3 == 0) {
      imm = imm * -1
    }

    SLTI(rs1, imm)
  }
}

class SLTIU(c: ALU) extends ALUTestBase(c) {
  private def SLTIU(rs1: Int, imm: Int) {
    // Turns out Scala doesn't have unsigned types so we do this trickery
    val u_rs1 = rs1.asInstanceOf[Long] & 0x00000000ffffffffL
    val u_imm = imm.asInstanceOf[Long] & 0x00000000ffffffffL
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.decoder_params.switch_2_imm, true)
    poke(c.io.in.decoder_params.imm, imm)
    poke(c.io.in.decoder_params.op, alu_ops.sltu)
    poke(c.io.in.decoder_params.op_code, op_code.Immediate)
    step(1)
    expect(c.io.result, u_rs1 < u_imm)
  }

  for (i <- 0 until 100) {
    var rs1 = rnd.nextInt(2000000000)
    var imm = rnd.nextInt(2000000000)

    val signedness = rnd.nextInt(50)
    if (signedness % 4 == 0) {
      rs1 = rs1 * -1
      imm = imm * -1
    } else if (signedness % 2 == 0) {
      rs1 = rs1 * -1
    } else if (signedness % 3 == 0) {
      imm = imm * -1
    }

    SLTIU(rs1, imm)
  }
}

class XORI(c: ALU) extends ALUTestBase(c) {
  private def XORI(rs1: Int, imm: Int) {
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.decoder_params.imm, imm)
    poke(c.io.in.decoder_params.switch_2_imm, true)
    poke(c.io.in.decoder_params.op, alu_ops.xor)
    poke(c.io.in.decoder_params.op_code, op_code.Immediate)
    step(1)
    expect(c.io.result, rs1 ^ imm)
  }

  for (i <- 0 until 100) {
    var rs1 = rnd.nextInt(2000000000)
    var imm = rnd.nextInt(2000000000)

    val signedness = rnd.nextInt(50)
    if (signedness % 4 == 0) {
      rs1 = rs1 * -1
      imm = imm * -1
    } else if (signedness % 2 == 0) {
      rs1 = rs1 * -1
    } else if (signedness % 3 == 0) {
      imm = imm * -1
    }

    XORI(rs1, imm)
  }
}

class SRLI(c: ALU) extends ALUTestBase(c) {
  private def SRLI(rs1: Int, imm: Int) {
    val special_imm =  31 & imm // h_0000_001f
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.decoder_params.switch_2_imm, true)
    poke(c.io.in.decoder_params.imm, special_imm)
    poke(c.io.in.decoder_params.op, alu_ops.sr)
    poke(c.io.in.decoder_params.op_code, op_code.Immediate)
    step(1)
    // >>> is the logic right shift operator
    expect(c.io.result, rs1 >>> special_imm)
  }

  for (i <- 0 until 100) {
    var rs1 = rnd.nextInt(2000000000)
    var imm = rnd.nextInt(2000000000)

    val signedness = rnd.nextInt(50)
    if (signedness % 4 == 0) {
      rs1 = rs1 * -1
      imm = imm * -1
    } else if (signedness % 2 == 0) {
      rs1 = rs1 * -1
    } else if (signedness % 3 == 0) {
      imm = imm * -1
    }

    SRLI(rs1, imm)
  }
}

class SRAI(c: ALU) extends ALUTestBase(c) {
  private def SRAI(rs1: Int, imm: Int) {
    val special_imm_2_shift =  31 & imm // h_0000_001f
    val special_imm =  1024 | special_imm_2_shift // h_0000_0400
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.decoder_params.switch_2_imm, true)
    poke(c.io.in.decoder_params.imm, special_imm)
    poke(c.io.in.decoder_params.op, alu_ops.sr)
    poke(c.io.in.decoder_params.op_code, op_code.Immediate)
    step(1)
    expect(c.io.result, rs1 >> special_imm_2_shift)
  }

  for (i <- 0 until 100) {
    var rs1 = rnd.nextInt(2000000000)
    var imm = rnd.nextInt(2000000000)

    val signedness = rnd.nextInt(50)
    if (signedness % 4 == 0) {
      rs1 = rs1 * -1
      imm = imm * -1
    } else if (signedness % 2 == 0) {
      rs1 = rs1 * -1
    } else if (signedness % 3 == 0) {
      imm = imm * -1
    }

    SRAI(rs1, imm)
  }
}

class ORI(c: ALU) extends ALUTestBase(c) {
  private def ORI(rs1: Int, imm: Int) {
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.decoder_params.switch_2_imm, true)
    poke(c.io.in.decoder_params.imm, imm)
    poke(c.io.in.decoder_params.op, alu_ops.or)
    poke(c.io.in.decoder_params.op_code, op_code.Immediate)
    step(1)
    expect(c.io.result, rs1 | imm)
  }

  for (i <- 0 until 100) {
    var rs1 = rnd.nextInt(2000000000)
    var imm = rnd.nextInt(2000000000)

    val signedness = rnd.nextInt(50)
    if (signedness % 4 == 0) {
      rs1 = rs1 * -1
      imm = imm * -1
    } else if (signedness % 2 == 0) {
      rs1 = rs1 * -1
    } else if (signedness % 3 == 0) {
      imm = imm * -1
    }

    ORI(rs1, imm)
  }
}

class ANDI(c: ALU) extends ALUTestBase(c) {
  private def ANDI(rs1: Int, imm: Int) {
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.decoder_params.switch_2_imm, true)
    poke(c.io.in.decoder_params.imm, imm)
    poke(c.io.in.decoder_params.op, alu_ops.and)
    poke(c.io.in.decoder_params.op_code, op_code.Immediate)
    step(1)
    expect(c.io.result, rs1 & imm)
  }

  for (i <- 0 until 100) {
    var rs1 = rnd.nextInt(2000000000)
    var imm = rnd.nextInt(2000000000)

    val signedness = rnd.nextInt(50)
    if (signedness % 4 == 0) {
      rs1 = rs1 * -1
      imm = imm * -1
    } else if (signedness % 2 == 0) {
      rs1 = rs1 * -1
    } else if (signedness % 3 == 0) {
      imm = imm * -1
    }

    ANDI(rs1, imm)
  }
}
