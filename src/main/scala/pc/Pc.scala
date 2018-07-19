// See LICENSE for license details.
package adept.pc

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig
import adept.idecode.OpCodes

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
  val op        = UInt(4.W)
  val br_offset = SInt(config.XLen.W)

  override def cloneType: this.type = {
    new DecoderPcIO(config).asInstanceOf[this.type]
  }

  private val pc_ops = PcOps
  def setDefaults = {
    op        := pc_ops.no_jmp
    br_offset := DontCare
  }
}

final object PcOps {
  val jal :: jalr :: beq :: bne :: blt :: bge :: bltu :: bgeu :: no_jmp :: Nil = Enum(9)

  private val op_codes = new OpCodes

  def getPcOp(funct: UInt, op_code: UInt) : UInt = {
    require(funct.getWidth == 3, "PC Operations are only 3 bits wide")
    require(op_code.getWidth == 7, "OP Codes are only 7 bits wide")

    val result = WireInit(0.U(4.W))

    when (op_code === op_codes.Branches) {
      when (funct === 0.U) {
        result := beq
      } .elsewhen (funct === 1.U) {
        result := bne
      } .elsewhen (funct === 4.U) {
        result := blt
      } .elsewhen (funct === 5.U) {
        result := bge
      } .elsewhen (funct === 6.U) {
        result := bltu
      } .otherwise {
        result := bgeu
      }
    } .elsewhen (op_code === op_codes.JAL) {
      result := jal
    } .elsewhen (op_code === op_codes.JALR) {
      // The check for a valid funct3 should be done at the decoder level.
      result := jalr
    } .otherwise {
      result := no_jmp
    }

    return result
  }

  def isBranch(op: UInt) : Bool = {
    return op === beq || op === bne || op === blt || op === bge || op === bltu || op === bgeu
  }
}

class Pc(config: AdeptConfig, br: BranchOpConstants) extends Module {
  val io = IO(new Bundle {
    // flag for branch confirmation
    val br_flag   = Input(Bool())
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

  val pc_ops = PcOps
  // Conditional Branch verification and flags attribution
  val cond_br_ver = MuxLookup (io.decoder.op, false.B,
                                Array(pc_ops.beq  -> ~io.br_flag,
                                      pc_ops.bne  -> io.br_flag,
                                      pc_ops.blt  -> io.br_flag,
                                      pc_ops.bge  -> ~io.br_flag,
                                      pc_ops.bltu -> io.br_flag,
                                      pc_ops.bgeu -> ~io.br_flag)
                              )

  val cond_br_exe   = pc_ops.isBranch(io.decoder.op) & cond_br_ver
  val offset_sel    = (io.decoder.op === pc_ops.jal) | (io.decoder.op === pc_ops.jalr) | cond_br_exe
  val add_to_pc_val = Mux(offset_sel,
                          io.decoder.br_offset,
                          4.S)

  val program_counter = RegInit("h_1000_0000".asUInt(config.XLen.W))

  val select_pc  = Mux(offset_sel,
                       io.pc_in,
                       program_counter).asSInt
  val pc_result  = Mux(io.decoder.op === pc_ops.jalr,
                       io.rs1,
                       select_pc) + add_to_pc_val
  // Remove LSB for JALR
  val jalr_value = pc_result & "h_FFFF_FFFE".U.asSInt
  val jalr_flag  = io.decoder.op === pc_ops.jalr
  // Next PC
  val next_pc    = Mux(jalr_flag,
                       jalr_value,
                       pc_result)

  // Logic to stall the next PC. When a control instruction is detected in the
  // decoder and ALU stage (if branch is taken):
  // - store the new PC
  // - invalidate the next instruction in the decoder in the next clock cycle.
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
            , Mux(io.decoder.op === pc_ops.jalr, io.rs1, select_pc)
            , add_to_pc_val
            , !stall_reg && !io.stall && (mem_en_reg ^ !io.mem_en))
  }

  if (config.sim && config.verbose >= 1) {
    when (io.success.getOrElse(false.B)) {
      printf("PC = 0x%x\n", program_counter)
    }
  }

}
