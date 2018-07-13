package adept.alu.reg

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.alu._

////////////////////////////////////////////////
// Test Suite for Register Type instructions
////////////////////////////////////////////////
class ADD(c: ALU) extends ALUTestBase(c) {
  private def ADD(rs1: Int, rs2: Int, imm: Int) {
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.registers.rs2, rs2)
    poke(c.io.in.decoder_params.switch_2_imm, false)
    poke(c.io.in.decoder_params.imm, 31 & imm)
    poke(c.io.in.decoder_params.op, alu_ops.add)
    poke(c.io.in.decoder_params.op_code, op_code.Registers)
    step(1)
    expect(c.io.result, rs1 + rs2)
  }

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

    ADD(rs1, rs2, imm)
  }
}

class SUB(c: ALU) extends ALUTestBase(c) {
  private def SUB(rs1: Int, rs2: Int, imm: Int) {
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.registers.rs2, rs2)
    poke(c.io.in.decoder_params.switch_2_imm, false)
    poke(c.io.in.decoder_params.imm, 1024)
    poke(c.io.in.decoder_params.op, alu_ops.add)
    poke(c.io.in.decoder_params.op_code, op_code.Registers)
    step(1)
    expect(c.io.result, rs1 - rs2)
  }

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

    SUB(rs1, rs2, imm)
  }
}

  // SLL
class SLL(c: ALU) extends ALUTestBase(c) {
  private def SLL(rs1: Int, rs2: Int, imm: Int) {
    val special_rs2 = 31 & rs2 // h_0000_001f
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.registers.rs2, special_rs2)
    poke(c.io.in.decoder_params.switch_2_imm, false)
    poke(c.io.in.decoder_params.imm, 31 & imm)
    poke(c.io.in.decoder_params.op, alu_ops.sll) // b001
    poke(c.io.in.decoder_params.op_code, op_code.Registers) // b0110011
    step(1)
    expect(c.io.result, rs1 << special_rs2)
  }

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

    SLL(rs1, rs2, imm)
  }
}

class SLT(c: ALU) extends ALUTestBase(c) {
  private def SLT(rs1: Int, rs2: Int, imm: Int) {
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.registers.rs2, rs2)
    poke(c.io.in.decoder_params.imm, 31 & imm)
    poke(c.io.in.decoder_params.switch_2_imm, false)
    poke(c.io.in.decoder_params.op, alu_ops.slt)
    poke(c.io.in.decoder_params.op_code, op_code.Registers)
    step(1)
    expect(c.io.result, rs1 < rs2)
  }

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

    SLT(rs1, rs2, imm)
  }
}

class SLTU(c: ALU) extends ALUTestBase(c) {
  private def SLTU(rs1: Int, rs2: Int, imm: Int) {
    // Turns out Scala doesn't have unsigned types so we do this trickery
    val u_rs1 = rs1.asInstanceOf[Long] & 0x00000000ffffffffL
    val u_rs2 = rs2.asInstanceOf[Long] & 0x00000000ffffffffL
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.registers.rs2, rs2)
    poke(c.io.in.decoder_params.imm, 31 & imm)
    poke(c.io.in.decoder_params.switch_2_imm, false)
    poke(c.io.in.decoder_params.op, alu_ops.sltu)
    poke(c.io.in.decoder_params.op_code, op_code.Registers)
    step(1)
    expect(c.io.result, u_rs1 < u_rs2)
  }

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

    SLTU(rs1, rs2, imm)
  }
}

class XOR(c: ALU) extends ALUTestBase(c) {
  private def XOR(rs1: Int, rs2: Int, imm: Int) {
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.registers.rs2, rs2)
    poke(c.io.in.decoder_params.imm, 31 & imm)
    poke(c.io.in.decoder_params.switch_2_imm, false)
    poke(c.io.in.decoder_params.op, alu_ops.xor)
    poke(c.io.in.decoder_params.op_code, op_code.Registers)
    step(1)
    expect(c.io.result, rs1 ^ rs2)
  }

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

    XOR(rs1, rs2, imm)
  }
}

class SRL(c: ALU) extends ALUTestBase(c) {
  private def SRL(rs1: Int, rs2: Int, imm: Int) {
    val special_rs2 =  31 & rs2 // h_0000_001f
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.registers.rs2, special_rs2)
    poke(c.io.in.decoder_params.imm, 31 & imm)
    poke(c.io.in.decoder_params.switch_2_imm, false)
    poke(c.io.in.decoder_params.op, alu_ops.sr)
    poke(c.io.in.decoder_params.op_code, op_code.Registers)
    step(1)
    // >>> is the logic right shift operator
    expect(c.io.result, rs1 >>> special_rs2)
  }

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

    SRL(rs1, rs2, imm)
  }
}

class SRA(c: ALU) extends ALUTestBase(c) {
  private def SRA(rs1: Int, rs2: Int, imm: Int) {
    val special_rs2_2_shift =  31 & rs2 // h_0000_001f
    val special_rs2 =  1024 | special_rs2_2_shift // h_0000_0400
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.registers.rs2, special_rs2)
    poke(c.io.in.decoder_params.switch_2_imm, false)
    poke(c.io.in.decoder_params.imm, 1024)
    poke(c.io.in.decoder_params.op, alu_ops.sr) // b101
    poke(c.io.in.decoder_params.op_code, op_code.Registers) // b0110011
    step(1)
    expect(c.io.result, rs1 >> special_rs2_2_shift)
  }

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

    SRA(rs1, rs2, imm)
  }
}

class OR(c: ALU) extends ALUTestBase(c) {
  private def OR(rs1: Int, rs2: Int, imm: Int) {
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.registers.rs2, rs2)
    poke(c.io.in.decoder_params.imm, 31 & imm)
    poke(c.io.in.decoder_params.switch_2_imm, false)
    poke(c.io.in.decoder_params.op, alu_ops.or)
    poke(c.io.in.decoder_params.op_code, op_code.Registers)
    step(1)
    expect(c.io.result, rs1 | rs2)
  }

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

    OR(rs1, rs2, imm)
  }
}

class AND(c: ALU) extends ALUTestBase(c) {
  private def AND(rs1: Int, rs2: Int, imm: Int) {
    poke(c.io.in.registers.rs1, rs1)
    poke(c.io.in.registers.rs2, rs2)
    poke(c.io.in.decoder_params.imm, 31 & imm)
    poke(c.io.in.decoder_params.switch_2_imm, false)
    poke(c.io.in.decoder_params.op, alu_ops.and)
    poke(c.io.in.decoder_params.op_code, op_code.Registers)
    step(1)
    expect(c.io.result, rs1 & rs2)
  }

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

    AND(rs1, rs2, imm)
  }
}
