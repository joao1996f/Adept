// See LICENSE for license details.
package adept.pc

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig
class BranchOpConstants { // join this group with all the rest of the configurations
  // Branch Type
  val BR_NE    = 1.asUInt(3.W)  // Branch on NotEqual
  val BR_EQ    = 0.asUInt(3.W)  // Branch on Equal
  val BR_GE    = 5.asUInt(3.W)  // Branch on Greater/Equal
  val BR_GEU   = 7.asUInt(3.W)  // Branch on Greater/Equal Unsigned
  val BR_LT    = 4.asUInt(3.W)  // Branch on Less Than
  val BR_LTU   = 6.asUInt(3.W)  // Branch on Less Than Unsigned

  val BR_Cond  = "b1100011".U
  val JAL   = "b1101111".U
  val JALR  = "b1100111".U
}

class Pc(config: AdeptConfig, br: BranchOpConstants) extends Module{
  val io = IO(new Bundle {
    // flags for branch confirmation
    val br_flags   = Input(Bool())
    // In from decoder
    val in_opcode  = Input(UInt(config.op_code.W)) // opcode(7 bits)
    val br_func    = Input(UInt(config.funct.W))   // function(3 bits)
    // Jump Address for JALR
    val br_step    = Input(UInt(config.XLen.W))
    // Offset for JAL or Conditional Branch
    val br_offset  = Input(SInt(config.XLen.W))
    // Program count after 1st pipeline level
    val pc_in      = Input(UInt(config.XLen.W))
    // Stall control signal
    val stall      = Input(Bool())
    // Memory enable control signal
    val mem_en     = Input(Bool())
    // Stall delayed by 1 clock
    val stall_reg  = Output(Bool())
    // Program count to be sent for calc of new PC or for storage
    val pc_out     = Output(UInt(config.XLen.W))
  })

  // Conditional Branch verification and flags attribution
  val cond_br_ver = MuxLookup (io.br_func, false.B,
    Array(br.BR_EQ  -> ~io.br_flags,
          br.BR_NE  -> io.br_flags,
          br.BR_LT  -> io.br_flags,
          br.BR_GE  -> ~io.br_flags,
          br.BR_LTU -> io.br_flags,
          br.BR_GEU -> ~io.br_flags))

  val cond_br_exe   = (io.in_opcode === br.BR_Cond) & cond_br_ver
  val offset_sel    = (io.in_opcode === br.JAL) | cond_br_exe
  val add_to_pc_val = Mux(offset_sel, io.br_offset, 4.S)

  // JALR condition verification
  val jalr_exec = (io.in_opcode === br.JALR)

  // Next PC
  val progCount = RegInit("h_1000_0000".asUInt(config.XLen.W))
  val next_pc   = Mux(offset_sel, io.pc_in, progCount).asSInt + add_to_pc_val

  // Remove LSB for JALR
  val jalr_value      = io.br_step & "hFFFFFFFE".U
  val jalrORpc_select = Mux(jalr_exec, jalr_value.asSInt, next_pc)

  // Logic to stall the next PC.
  // When a control instruction is detected in the decoder and ALU stage, store the new PC
  // (if branch is taken) and then invalidate the next instruction in the decoder in the next
  // clock cycle.
  val stall_reg  = RegInit(false.B)
  stall_reg     := offset_sel | jalr_exec
  io.stall_reg  := stall_reg

  // Delay memory enable by one cycle
  val mem_en_reg = RegInit(false.B)
  mem_en_reg    := io.mem_en

  // PC update
  when (!stall_reg && !io.stall && (mem_en_reg ^ !io.mem_en)) {
    progCount := jalrORpc_select.asUInt
  }

  io.pc_out  := progCount
}
