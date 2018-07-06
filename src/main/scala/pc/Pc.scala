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

class DecoderPcIO(val config: AdeptConfig) extends Bundle {
  val br_op     = UInt(config.funct.W)
  val br_offset = SInt(config.XLen.W)

  override def cloneType: this.type = {
    new DecoderPcIO(config).asInstanceOf[this.type]
  }

  def setDefaults = {
    br_op     := DontCare
    br_offset := DontCare
  }
}

class Pc(config: AdeptConfig, br: BranchOpConstants) extends Module {
  val io = IO(new Bundle {
    // flag for branch confirmation
    val br_flag   = Input(Bool())
    // In from decoder
    val in_opcode = Input(UInt(config.op_code.W))
    // Value of RS1 used in JALR
    val rs1       = Input(SInt(config.XLen.W))
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
    // Decoder control signals
    val decoder   = Input(new DecoderPcIO(config))

    // Used in simulation only to print the PC at the end
    val success = if (config.sim) {
      Some(Input(Bool()))
    } else {
      None
    }
  })

  // Conditional Branch verification and flags attribution
  val cond_br_ver = MuxLookup (io.decoder.br_op, false.B,
    Array(br.BEQ  -> ~io.br_flag,
          br.BNE  -> io.br_flag,
          br.BLT  -> io.br_flag,
          br.BGE  -> ~io.br_flag,
          br.BLTU -> io.br_flag,
          br.BGEU -> ~io.br_flag))

  val cond_br_exe   = (io.in_opcode === br.BR) & cond_br_ver
  val offset_sel    = (io.in_opcode === br.JAL) | (io.in_opcode === br.JALR) | cond_br_exe
  val add_to_pc_val = Mux(offset_sel, io.decoder.br_offset, 4.S)

  val program_counter = RegInit("h_1000_0000".asUInt(config.XLen.W))

  val select_pc  = Mux(offset_sel, io.pc_in, program_counter).asSInt
  val pc_result  = Mux(io.in_opcode === br.JALR, io.rs1, select_pc) + add_to_pc_val
  // Remove LSB for JALR
  val jalr_value = pc_result & "h_FFFF_FFFE".U.asSInt
  val jalr_flag  = io.in_opcode === br.JALR
  // Next PC
  val next_pc    = Mux(jalr_flag, jalr_value, pc_result)

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

  if (config.sim && config.verbose >= 2) {
    printf("PC\n")
    printf("Current PC=[0x%x], New PC=[0x%x, A=0x%x, B=0x%x], PC En=[%b]\n"
            , program_counter
            , next_pc
            , Mux(io.in_opcode === br.JALR, io.rs1, select_pc)
            , add_to_pc_val
            , !stall_reg && !io.stall && (mem_en_reg ^ !io.mem_en))
  }

  if (config.sim && config.verbose >= 1) {
    when (io.success.getOrElse(false.B)) {
      printf("PC = 0x%x\n", program_counter)
    }
  }

}
