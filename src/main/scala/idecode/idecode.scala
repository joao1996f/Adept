package adept.idecode

import chisel3._

import adept.config.AdeptConfig

class DecoderALUOut(val config: AdeptConfig) extends Bundle {
  // Immediate, is sign extended
  val imm     = Output(SInt(config.XLen.W))
  // Operation
  val op      = Output(UInt(config.funct.W))
  val op_code = Output(UInt(config.op_code.W))

  override def cloneType: this.type = {
    new DecoderALUOut(config).asInstanceOf[this.type];
  }
}

class DecoderRegisterOut(val config: AdeptConfig) extends Bundle {
  // Registers
  val rs1_sel = Output(UInt(config.rs_len.W))
  val rs2_sel = Output(UInt(config.rs_len.W))
  val rsd_sel = Output(UInt(config.rs_len.W))

  override def cloneType: this.type = {
    new DecoderRegisterOut(config).asInstanceOf[this.type];
  }
}

class InstructionDecoder(config: AdeptConfig) extends Module {
  val io = IO(new Bundle{
                // Input
                val instruction = Input(UInt(config.XLen.W))

                // Output
                val registers = new DecoderRegisterOut(config)
                val alu       = new DecoderALUOut(config)
              })

  // BTW this is a bad implementation, but its OK to start off.
  // Optimizations will be done down the line.

  // Only support ALU instructions with immediates right now
  // OP Code: 0010011 (6 dw 0) of instruction
  when (io.instruction(6, 0) === "b0010011".U) {
    io.registers.rs1_sel := io.instruction(16, 20)
    // Shift instructions don't have rs2. In that case rs2 contains the shift
    // amount.
    io.registers.rs2_sel := io.instruction(12, 16)
    io.registers.rsd_sel := io.instruction(24, 28)
    // Shift instructions have a special code in the immediate, in the ALU check
    // the two LSBs of the OP
    io.alu.imm := io.instruction(0, 11)
    io.alu.op := io.instruction(21, 23)
    io.alu.op_code := io.instruction(26, 31)
  }
}
