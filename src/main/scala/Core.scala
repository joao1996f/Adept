package adept.core

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig
import adept.idecode.InstructionDecoder
import adept.registerfile.RegisterFile
import adept.mem.Memory
import adept.pc.BranchOpConstants
import adept.pc.Pc
import adept.instructionMemoryRom.InstrMemRom
import adept.alu.ALU

class Adept(config: AdeptConfig) extends Module {
  val io = IO(new Bundle{})

  //////////////////////////////////////////////////////////////////////////////
  // Create Modules
  //////////////////////////////////////////////////////////////////////////////
  // Instruction Fetch
  val pc = Module(new Pc(config, new BranchOpConstants))

  // Program ROM
  val rom = Module(new InstrMemRom(config))

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
  // Instruction Fetch connections
  pc.io.br_flags  := alu.io.cmp_flag
  pc.io.in_opcode := Cat(idecode.io.alu.op, idecode.io.alu.op_code)
  pc.io.br_step   := alu.io.result
  pc.io.br_offset := idecode.io.imm_b_offset

  // Program ROM connections
  rom.io.in_pc           := pc.io.pc_out
  idecode.io.instruction := rom.io.instr

  // Register File
  register_file.io.decoder <> idecode.io.registers

  // MUX Selections to Operands in ALU
  alu.io.in.registers.rs1 := MuxLookup(idecode.io.sel_operand_a, 0.S, Array(
                                   0.U -> register_file.io.registers.rs1,
                                   1.U -> pc.io.pc_out.asSInt))

  alu.io.in.registers.rs2 := register_file.io.registers.rs2
  alu.io.in.decoder_params <> idecode.io.alu

  // Memory Connections
  mem.io.in.data_in := register_file.io.registers.rs2
  mem.io.in.addr_w  := alu.io.result.asUInt
  mem.io.in.addr_r  := alu.io.result.asUInt
  mem.io.decode     <> idecode.io.mem

  // MUX Selections to Register File
  register_file.io.rsd_value := MuxLookup(idecode.io.sel_rf_wb, 0.S,
                                          Array(
                                            0.U -> alu.io.result,
                                            1.U -> mem.io.data_out
                                          ))

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
           , rom.io.instr
)
}

// This is needed to generate the verilog just for this module. When generating
// the verilog this object will only be needed in the top module.
object Adept extends App {
  val config = new AdeptConfig
  chisel3.Driver.execute(args, () => new Adept(config))
}
