package adept.core

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig
import adept.idecode.InstructionDecoder
import adept.registerfile.RegisterFile
import adept.mem.Memory
import adept.pc.BranchOpConstants
import adept.pc.Pc
import adept.instructionMemory.InstrMem
import adept.alu.ALU

class Adept(config: AdeptConfig) extends Module {
  val io = IO(new Bundle{
                // Inputs
                val data_in = Input(UInt(config.XLen.W))
                val addr_w = Input(UInt(config.XLen.W))
                val we = Input(Bool())

                // Outputs
                val success = Output(Bool())
              })

  //////////////////////////////////////////////////////////////////////////////
  // Create Modules
  //////////////////////////////////////////////////////////////////////////////
  // Instruction Fetch
  val pc = Module(new Pc(config, new BranchOpConstants))

  // Program ROM
  val mem_instr = Module(new InstrMem(config))

  // Instruction Decoder
  val idecode = Module(new InstructionDecoder(config))

  // Register File
  val register_file = Module(new RegisterFile(config))

  // ALU
  val alu = Module(new ALU(config))

  // Memory
  val mem_data = Module(new Memory(config))

  //////////////////////////////////////////////////////////////////////////////
  // Connections
  //////////////////////////////////////////////////////////////////////////////

  ///////////////////////////////////////////////////////////////////
  // Instruction Fetch Stage
  ///////////////////////////////////////////////////////////////////
  pc.io.br_flags  := alu.io.cmp_flag
  pc.io.in_opcode := Cat(idecode.io.br_op, idecode.io.alu.op_code)
  pc.io.br_step   := alu.io.result
  pc.io.br_offset := idecode.io.imm_b_offset
  pc.io.stall     := idecode.io.stall
  pc.io.stall_re  := idecode.io.stall_re

  ///////////////////////////////////////////////////////////////////
  // Decode, Execute and Memory Stage
  ///////////////////////////////////////////////////////////////////
  // Program connections
  mem_instr.io.in_pc           := pc.io.pc_out
  mem_instr.io.data_in         := io.data_in
  mem_instr.io.addr_w          := io.addr_w
  mem_instr.io.we              := io.we
  /*val rst = WireInit(false.B)
  rst := RegNext(true.B)*/
  idecode.io.instruction       := mem_instr.io.instr

  // Register File
  register_file.io.decoder.rs1_sel := idecode.io.registers.rs1_sel
  register_file.io.decoder.rs2_sel := idecode.io.registers.rs2_sel

  // Forwarding Path Control Logic
  val sel_frw_path_rs1 = Wire(Bool())
  val sel_frw_path_rs2 = Wire(Bool())
  val write_back       = Wire(SInt(32.W))

  // Pipeline PC
  val ex_pc = RegNext(pc.io.pc_out)
  pc.io.pc_in     := ex_pc
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
  mem_data.io.in.data_in := register_file.io.registers.rs2
  mem_data.io.in.addr    := alu.io.result.asUInt
  mem_data.io.decode     <> idecode.io.mem

  ///////////////////////////////////////////////////////////////////
  // Write Back Stage
  ///////////////////////////////////////////////////////////////////

  val rsd_sel_wb = RegInit(0.U(config.rs_len.W))
  rsd_sel_wb := idecode.io.registers.rsd_sel
  val we_wb      = RegInit(false.B)
  we_wb := idecode.io.registers.we

  // MUX Selections to Register File
  write_back := MuxLookup(RegNext(idecode.io.sel_rf_wb), 0.S,
                          Array(
                            0.U -> RegNext(alu.io.result),
                            // Already delayed by one cycle
                            1.U -> mem_data.io.data_out
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

  // Condition for simulation termination is
  // loop:
  //   j loop
  // This might change to an unaligned instruction access exception
  val prev_instr = RegInit(100.U)
  val prev_instr2 = RegInit(100.U)
  prev_instr := mem_instr.io.instr
  prev_instr2 := prev_instr
  io.success := prev_instr2 === mem_instr.io.instr

  // Debug
  // Stole this fmem_instr Sodor
  // https://github.com/ucb-bar/riscv-sodor/blob/master/src/rv32_1stage/dpath.scala#L196
  printf("Op1=[0x%x] Op2=[0x%x] W[%d,%d= 0x%x] Mem[%d: R:0x%x W:0x%x] DASM(%x)\n"
           , alu.io.in.registers.rs1
           , alu.io.in.registers.rs2
           , idecode.io.registers.we
           , idecode.io.registers.rsd_sel
           , register_file.io.rsd_value
           , idecode.io.sel_rf_wb
           , mem_data.io.data_out
           , mem_data.io.in.data_in
           , mem_instr.io.instr
)
}

// This is needed to generate the verilog just for this module. When generating
// the verilog this object will only be needed in the top module.
object Adept extends App {
  val config = new AdeptConfig
  chisel3.Driver.execute(args, () => new Adept(config))
}
