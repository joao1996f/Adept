// See LICENSE for license details.
package adept.pc

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig

class BranchOpConstants {
  // Branch Type
  val BNE  = 1.U // Branch on NotEqual
  val BEQ  = 0.U // Branch on Equal
  val BGE  = 5.U // Branch on Greater/Equal
  val BGEU = 7.U // Branch on Greater/Equal Unsigned
  val BLT  = 4.U // Branch on Less Than
  val BLTU = 6.U // Branch on Less Than Unsigned

  val BR   = "b1100011".U
  val JAL  = "b1101111".U
  val JALR = "b1100111".U
}

class Pc(config: AdeptConfig, br: BranchOpConstants) extends Module {
  val io = IO(new Bundle {
    // flag for branch confirmation
    val br_flag   = Input(Bool())
    // In from decoder
    val in_opcode = Input(UInt(config.op_code.W)) // opcode(7 bits)
    val br_func   = Input(UInt(config.funct.W))   // function(3 bits)
    // Jump Address for JALR
    val br_step   = Input(SInt(config.XLen.W))
    // Offset for JAL or Conditional Branch
    val br_offset = Input(SInt(config.XLen.W))
    // Program count after 1st pipeline level
    val pc_in     = Input(UInt(config.XLen.W))
    // Stall control signal
    val stall     = Input(Bool())
    // Memory enable control signal
    val mem_en    = Input(Bool())
    // Stall delayed by 1 clock
    val stall_reg = Output(Bool())
    // Program count to be sent for calc of new PC or for storage
    val pc_out    = Output(UInt(config.XLen.W))
  })

  // Conditional Branch verification and flags attribution
  val cond_br_ver = MuxLookup (io.br_func, false.B,
    Array(br.BEQ  -> ~io.br_flag,
          br.BNE  -> io.br_flag,
          br.BLT  -> io.br_flag,
          br.BGE  -> ~io.br_flag,
          br.BLTU -> io.br_flag,
          br.BGEU -> ~io.br_flag))

  val cond_br_exe   = (io.in_opcode === br.BR) & cond_br_ver
  val offset_sel    = (io.in_opcode === br.JAL) | cond_br_exe
  val add_to_pc_val = Mux(offset_sel, io.br_offset, 4.S)

  val program_counter = RegInit("h_1000_0000".asUInt(config.XLen.W))

  // Remove LSB for JALR
  val jalr_value = io.br_step & "h_FFFF_FFFE".U.asSInt
  val add_to_pc_res   = Mux(offset_sel, io.pc_in, program_counter).asSInt + add_to_pc_val
  // Next PC
  val jalr_flag  = io.in_opcode === br.JALR
  val next_pc    = Mux(jalr_flag, jalr_value, add_to_pc_res)

  // Logic to stall the next PC.
  // When a control instruction is detected in the decoder and ALU stage, store the new PC
  // (if branch is taken) and then invalidate the next instruction in the decoder in the next
  // clock cycle.
  val stall_reg  = RegInit(false.B)
  stall_reg     := offset_sel | jalr_flag
  io.stall_reg  := stall_reg

  // Delay memory enable by one cycle
  val mem_en_reg = RegInit(false.B)
  mem_en_reg    := io.mem_en

  // PC update
  when (!stall_reg && !io.stall && (mem_en_reg ^ !io.mem_en)) {
    program_counter := next_pc.asUInt
  }

  io.pc_out := program_counter
}
