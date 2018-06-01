// See LICENSE for license details.
package adept.core

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig
import adept.idecode.InstructionDecoder
import adept.registerfile.RegisterFile
import adept.mem.Memory
import adept.pc.BranchOpConstants
import adept.pc.Pc
import adept.alu.ALU

class MemLoadIO(config: AdeptConfig) extends Bundle {
  // Inputs
  val data_in = Input(Vec(4, UInt(8.W)))
  val addr_w  = Input(UInt(config.XLen.W))
  val we      = Input(Bool())

  override def cloneType: this.type = {
    new MemLoadIO(config).asInstanceOf[this.type]
  }
}

class Adept(config: AdeptConfig) extends Module {
  val io = IO(new Bundle{
                // Load program interface
                val load = new MemLoadIO(config)

                // Outputs
                val success = Output(Bool())
              })

  //////////////////////////////////////////////////////////////////////////////
  // Create Modules
  //////////////////////////////////////////////////////////////////////////////
  // Instruction Fetch
  val pc = Module(new Pc(config, new BranchOpConstants))

  // Instruction Decoder
  val idecode = Module(new InstructionDecoder(config))

  // Register File
  val register_file = Module(new RegisterFile(config))

  // ALU
  val alu = Module(new ALU(config))

  // Memory
  val mem = Module(new Memory(config))

  //////////////////////////////////////////////////////////////////////////////
  // Connections
  //////////////////////////////////////////////////////////////////////////////
  val stall = WireInit(false.B)
  stall := (mem.io.stall & idecode.io.mem.en) | pc.io.stall_reg

  // Pipeline PC
  val ex_pc = RegInit(0.S)
  when (!stall) {
    ex_pc := pc.io.pc_out.asSInt
  }

  ///////////////////////////////////////////////////////////////////
  // Instruction Fetch Stage
  ///////////////////////////////////////////////////////////////////
  pc.io.br_flags  := alu.io.cmp_flag
  pc.io.in_opcode := idecode.io.alu.op_code
  pc.io.br_func   := idecode.io.br_op
  pc.io.br_step   := alu.io.result.asUInt
  pc.io.br_offset := idecode.io.imm_b_offset
  pc.io.pc_in     := ex_pc.asUInt
  pc.io.stall := stall
  pc.io.mem_en    := idecode.io.mem.en

  ///////////////////////////////////////////////////////////////////
  // Decode, Execute and Memory Stage
  ///////////////////////////////////////////////////////////////////
  // Program connections
  mem.io.pc_in   := pc.io.pc_out
  mem.io.load <> io.load
  val rst        = RegInit(false.B)
  rst            := true.B

  // Store the previous instruction in the first rising edge the memory
  // instruction is enabled. Ignore all others
  val prev_instr               = RegInit(0.U)
  val prev_instr_1delay_stall  = RegInit(false.B)
  prev_instr_1delay_stall     := idecode.io.mem.en
  when ((idecode.io.mem.en && !prev_instr_1delay_stall) || !stall) {
    prev_instr := mem.io.instr_out
  }

  idecode.io.instruction := Mux(mem.io.stall, prev_instr, mem.io.instr_out & Fill(config.XLen, rst))
  idecode.io.stall_reg   := pc.io.stall_reg

  // Register File
  register_file.io.decoder.rs1_sel := idecode.io.registers.rs1_sel
  register_file.io.decoder.rs2_sel := idecode.io.registers.rs2_sel

  // Forwarding Path Control Logic
  val sel_frw_path_rs1 = Wire(Bool())
  val sel_frw_path_rs2 = Wire(Bool())
  val write_back       = Wire(SInt(32.W))

  // MUX Selections to Operands in ALU
  val sel_rs1 = Mux(sel_frw_path_rs1, 2.U, idecode.io.sel_operand_a)
  alu.io.in.registers.rs1 := MuxLookup(sel_rs1, 0.S,
                                       Array(
                                          0.U -> register_file.io.registers.rs1,
                                          1.U -> ex_pc.asSInt,
                                          2.U -> write_back
                                       ))

  alu.io.in.registers.rs2 := Mux(sel_frw_path_rs2,
                                 write_back, register_file.io.registers.rs2)
  alu.io.in.decoder_params <> idecode.io.alu

  // Memory Connections
  mem.io.in.data_in := register_file.io.registers.rs2
  mem.io.in.addr    := alu.io.result.asUInt
  mem.io.decode     <> idecode.io.mem

  ///////////////////////////////////////////////////////////////////
  // Write Back Stage
  ///////////////////////////////////////////////////////////////////
  val rsd_sel_wb = RegInit(0.U)
  val we_wb      = RegInit(false.B)
  val alu_res_wb = RegInit(0.S)
  val sel_rf_wb  = RegInit(0.U)

  val stall_wb_reg = RegInit(false.B)
  stall_wb_reg    := stall

  when (!stall_wb_reg) {
    rsd_sel_wb := idecode.io.registers.rsd_sel
    we_wb      := idecode.io.registers.we
    alu_res_wb := alu.io.result
    sel_rf_wb  := idecode.io.sel_rf_wb
  }

  // MUX Selections to Register File
  write_back := MuxLookup(sel_rf_wb, 0.S,
                          Array(
                            0.U -> alu_res_wb,
                            // Already delayed by one cycle
                            1.U -> mem.io.data_out
                          ))
  register_file.io.rsd_value       := write_back
  register_file.io.decoder.rsd_sel := rsd_sel_wb
  register_file.io.decoder.we      := we_wb

  // Forwarding Path Control Logic
  sel_frw_path_rs1 := rsd_sel_wb === idecode.io.registers.rs1_sel &&
    rsd_sel_wb =/= 0.U && we_wb
  sel_frw_path_rs2 := rsd_sel_wb === idecode.io.registers.rs2_sel &&
    rsd_sel_wb =/= 0.U && we_wb

  ///////////////////////////////////////////////////////////////////
  // Debug Stuff
  ///////////////////////////////////////////////////////////////////

  // Simulation ends when program detects a write of 0xdead0000 to address
  // 0x00000000
  if (config.sim) {
    io.success := mem.io.instr_out === "h_dead_0737".U
  } else {
    io.success := false.B
  }

  // Debug
  // Stole this from Sodor
  // https://github.com/ucb-bar/riscv-sodor/blob/master/src/rv32_1stage/dpath.scala#L196
  printf("Op1=[0x%x] Op2=[0x%x] W[%d,%d= 0x%x] Mem[%d: R:0x%x W:0x%x] DASM(%x)\n"
           , alu.io.in.registers.rs1
           , alu.io.in.registers.rs2
           , idecode.io.registers.we
           , idecode.io.registers.rsd_sel
           , register_file.io.rsd_value
           , idecode.io.sel_rf_wb
           , mem.io.data_out
           , mem.io.in.data_in
           , mem.io.instr_out)
}

object Adept extends App {
  val config = new AdeptConfig
  chisel3.Driver.execute(args, () => new Adept(config))
}
