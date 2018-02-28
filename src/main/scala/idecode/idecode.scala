package adept.idecode

import chisel3._

import adept.config.AdeptConfig

class InstructionDecoder(config: AdeptConfig) extends Module {
  val io = IO(new Bundle{
                // Input
                val instruction = Input(UInt(config.XLen.W))

                // Output
                // Registers
                val rs1 = Output(UInt(config.rs_len.W))
                val rs2 = Output(UInt(config.rs_len.W))
                val rsd = Output(UInt(config.rs_len.W))

                // Immediate, is sign extended
                val imm = Output(SInt(config.XLen.W))
                // Operation
                val op = Output(UInt(config.funct.W))
                val op_code = Output(UInt(config.op_code.W))
              })

  // BTW this is a bad implementation, but its OK to start off.
  // Optimizations will be done down the line.

  // Only support ALU instructions with immediates right now
  // OP Code: 0010011 (6 dw 0) of instruction
  when (io.instruction(26, 31) === "b0010011".U) {
    io.rs1 := io.instruction(16, 20)
    // Shift instructions don't have rs2. In that case rs2 contains the shift
    // amount.
    io.rs2 := io.instruction(12, 16)
    io.rsd := io.instruction(24, 28)
    // Shift instructions have a special code in the immediate, in the ALU check
    // the two LSBs of the OP
    io.imm := io.instruction(0, 11)
    io.op := io.instruction(21, 23)
    io.op_code := io.instruction(26, 31)
  }
}
