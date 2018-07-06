// See LICENSE for license details.
package adept.idecode

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig
import adept.mem.DecoderMemIO
import adept.alu.DecoderAluIO
import adept.registerfile.DecoderRegisterFileIO
import adept.pc.DecoderPcIO

////////////////////////////////////////////////////////////////////////////////
// BE WARNED! THIS IS TERRIBLE CODE, READ AT YOUR OWN PERIL!
////////////////////////////////////////////////////////////////////////////////
class InstructionDecoder(config: AdeptConfig) extends Module {
  val io = IO(new Bundle{
                // Input
                val instruction = Input(UInt(config.XLen.W))
                val stall_reg   = Input(Bool())

                // Output
                val registers = Output(new DecoderRegisterFileIO(config))
                val alu       = Output(new DecoderAluIO(config))
                val mem       = Output(new DecoderMemIO(config))
                val pc        = Output(new DecoderPcIO(config))

                // ALU selection control signals
                val sel_operand_a = Output(UInt(1.W))
                // Write Back selection signals
                val sel_rf_wb     = Output(UInt(1.W))
              })

  // BTW this is a bad implementation, but its OK to start off.
  // Optimizations will be done down the line.
  val instruction = io.instruction
  val op_code    = Wire(UInt(7.W))
  val rsd_sel    = instruction(11, 7)
  val op         = instruction(14, 12)
  val rs1_sel    = instruction(19, 15)
  val rs2_sel    = instruction(24, 20)
  val imm        = instruction(31, 20)
  val mem_en     = Wire(Bool())

  // Send OP to the branch execute module
  io.pc.br_op    := op

  // Ignore current instruction when the previous was a control instruction
  op_code        := Mux(io.stall_reg, 0.U, instruction(6, 0))

  io.alu.op_code := op_code
  io.mem.en      := mem_en

  //////////////////////////////////////////////////////
  // I-Type Decode => OP Code: 0010011 of instruction for immediate and 0000011
  // Load instructions and 1100011 for JALR
  //////////////////////////////////////////////////////
  when (op_code === "b0010011".U || op_code === "b0000011".U || op_code === "b1100111".U) {
    io.registers.rs1_sel := rs1_sel
    // Shift instructions don't have rs2. In that case rs2 contains the shift
    // amount.
    io.registers.rs2_sel := rs2_sel
    io.registers.rsd_sel := rsd_sel
    // Shift instructions have a special code in the immediate, in the ALU check
    // the two LSBs of the OP
    io.alu.switch_2_imm := true.B
    io.registers.we     := true.B
    io.mem.we           := false.B
    // Selects the ALU result to be written to the Register File when it is not
    // a load instruction
    when (op_code === "b0010011".U) {
      // Immediate
      io.sel_operand_a := 0.U
      io.pc.br_offset  := 0.S
      io.alu.imm       := imm.asSInt
      io.sel_rf_wb     := 0.U
      io.alu.op        := op
      io.mem.op        := 0.U
      mem_en           := false.B
    } .elsewhen (op_code === "b1100111".U) {
      // JALR
      io.sel_operand_a := 1.U // Operand A is PC
      io.alu.imm       := 4.S // Set immediate
      io.alu.op        := 0.U // ALU: PC + 4
      io.pc.br_offset  := imm.asSInt // Pass immediate to PC
      io.sel_rf_wb     := 0.U
      io.mem.op        := 0.U
      mem_en           := false.B
    } .otherwise {
      // Load
      io.sel_operand_a := 0.U
      io.pc.br_offset  := 0.S
      io.alu.imm       := imm.asSInt
      io.sel_rf_wb     := 1.U // Select the Memory to write to the register file
      io.alu.op        := 0.U // Always perform an ADD when it's a Load
      io.mem.op        := op
      mem_en           := true.B
    }
  }
  //////////////////////////////////////////////////////
  // R-Type Decode => OP Code: 0110011 of instruction
  //////////////////////////////////////////////////////
    .elsewhen (op_code === "b0110011".U) {
    io.registers.rs1_sel := rs1_sel
    io.registers.rs2_sel := rs2_sel
    io.registers.rsd_sel := rsd_sel
    // Shift instructions and Add/Sub have a special code in the immediate, in
    // the ALU check the two LSBs of the OP
    io.alu.imm      := imm.asSInt
    io.alu.op       := op
    io.alu.switch_2_imm := false.B
    io.pc.br_offset := 0.S
    io.registers.we := true.B
    // Select RS1 and write the ALU result to the register file
    io.sel_operand_a := 0.U
    io.sel_rf_wb     := 0.U
    io.mem.we        := false.B
    io.mem.op        := 0.U
    mem_en           := false.B
  }
  //////////////////////////////////////////////////////
  // S-Type Decode => OP Code: 0100011 of instruction
  //////////////////////////////////////////////////////
    .elsewhen (op_code === "b0100011".U) {
    io.registers.rs1_sel := rs1_sel
    io.registers.rs2_sel := rs2_sel
    io.registers.rsd_sel := 0.U
    io.alu.switch_2_imm  := true.B
    io.alu.imm           := Cat(imm(11, 5), rsd_sel).asSInt
    io.alu.op            := 0.U // Perform ADD in the ALU between rs1 and the immediate
    io.registers.we      := false.B
    io.pc.br_offset      := 0.S
    io.sel_operand_a     := 0.U
    // This is don't care. Register File write enable is set to false
    io.sel_rf_wb         := 0.U
    io.mem.we            := true.B
    io.mem.op            := op
    mem_en               := true.B
  }
  //////////////////////////////////////////////////////
  // B-Type Decode => OP Code: 1100011 of instruction
  //////////////////////////////////////////////////////
  .elsewhen (op_code === "b1100011".U) {
    io.registers.rs1_sel := rs1_sel
    io.registers.rs2_sel := rs2_sel
    io.registers.rsd_sel := 0.U
    io.pc.br_offset      := Cat(imm(11), rsd_sel(0), imm(10, 5), rsd_sel(4, 1), 0.asUInt(1.W)).asSInt
    io.alu.imm           := 1024.S
    io.alu.switch_2_imm  := false.B

    when (op === "b000".U || op === "b001".U) {
      io.alu.op := "b000".U
    } .elsewhen (op === "b100".U || op === "b101".U) {
      io.alu.op := "b010".U
    } .otherwise {
      io.alu.op := "b011".U
    }

    io.registers.we  := false.B
    io.sel_operand_a := 0.U
    // This is don't care. Register File write enable is set to false
    io.sel_rf_wb     := 0.U
    io.mem.we        := false.B
    io.mem.op        := 0.U
    mem_en           := false.B
  }
  //////////////////////////////////////////////////////
  // U-Type Decode => OP Code: 0010111 or 0110111 of instruction
  //////////////////////////////////////////////////////
    .elsewhen (op_code === "b0010111".U || op_code === "b0110111".U) {
    io.registers.rs1_sel := 0.U
    io.registers.rs2_sel := 0.U
    io.registers.rsd_sel := rsd_sel
    io.alu.imm           := Cat(imm, rs1_sel, op, Fill(12, "b0".U)).asSInt
    io.alu.op            := 0.U
    io.pc.br_offset      := 0.S
    io.registers.we      := true.B

    when (op_code(5) === false.B) {
    // AUIPC
      io.alu.switch_2_imm := false.B
      io.sel_operand_a    := 1.U
    } .otherwise {
    // JALR
      io.alu.switch_2_imm := true.B
      io.sel_operand_a    := 0.U
    }

    io.sel_rf_wb     := 0.U
    io.mem.we        := false.B
    io.mem.op        := 0.U
    mem_en           := false.B
  }
  //////////////////////////////////////////////////////
  // J-Type Decode => OP Code: 1101111 of instruction
  //////////////////////////////////////////////////////
    .elsewhen(op_code === "b1101111".U) {
    io.registers.rs1_sel := 0.U
    io.registers.rs2_sel := 0.U
    io.registers.rsd_sel := rsd_sel
    io.pc.br_offset      := Cat(imm(11), rs1_sel, op, imm(0), imm(10, 1), 0.asUInt(1.W)).asSInt
    io.alu.switch_2_imm  := true.B
    io.alu.imm           := 4.S
    io.alu.op            := 0.U
    io.registers.we      := true.B
    io.sel_operand_a     := 1.U
    io.sel_rf_wb         := 0.U
    io.mem.we            := false.B
    io.mem.op            := 0.U
    mem_en               := false.B
  }
  //////////////////////////////////////////////////////
  // Invalid Instruction executes a NOP
  //////////////////////////////////////////////////////
  .otherwise{
    io.registers.rs1_sel := 0.U
    io.registers.rs2_sel := 0.U
    io.registers.rsd_sel := rsd_sel
    io.pc.br_offset      := 0.S
    io.alu.switch_2_imm  := false.B
    io.alu.imm           := 0.S
    io.alu.op            := 0.U
    io.registers.we      := false.B
    io.sel_operand_a     := 0.U
    io.sel_rf_wb         := 0.U
    io.mem.we            := false.B
    io.mem.op            := 0.U
    mem_en               := false.B
  }
}
