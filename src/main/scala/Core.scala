// See LICENSE for license details.
package adept.core

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig
import adept.idecode.InstructionDecoder
import adept.registerfile.RegisterFile
import adept.mem.Memory
import adept.mem.MemLoadIO
import adept.pc.Pc
import adept.alu.ALU

class Adept(config: AdeptConfig) extends Module {
  val io = IO(new Bundle{
                // Load program interface
                val load = new MemLoadIO(config)

                // Outputs
                val success = Output(Bool())
                val trap    = Output(Bool())
              })

  //////////////////////////////////////////////////////////////////////////////
  // Create Modules
  //////////////////////////////////////////////////////////////////////////////
  // Instruction Fetch
  val pc = Module(new Pc(config))

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
  stall := (mem.io.stall & idecode.io.basic.out.mem.en) | pc.io.stall_reg

  // Pipeline PC
  val ex_pc = RegInit(0.S)
  when (!stall) {
    ex_pc := pc.io.pc_out.asSInt
  }

  // Forwarding Path Control Logic
  val sel_frw_path_rs1 = Wire(Bool())
  val sel_frw_path_rs2 = Wire(Bool())
  val write_back       = Wire(SInt(32.W))

  ///////////////////////////////////////////////////////////////////
  // Instruction Fetch Stage
  ///////////////////////////////////////////////////////////////////
  pc.io.decoder <> idecode.io.basic.out.pc
  pc.io.flag    := alu.io.cmp_flag
  pc.io.rs1     := Mux(sel_frw_path_rs1,
                       write_back,
                       register_file.io.registers.rs1)
  pc.io.pc_in   := ex_pc.asUInt
  pc.io.stall   := stall
  pc.io.mem_en  := idecode.io.basic.out.mem.en

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
  prev_instr_1delay_stall     := idecode.io.basic.out.mem.en
  when ((idecode.io.basic.out.mem.en && !prev_instr_1delay_stall) || !stall) {
    prev_instr := mem.io.instr_out
  }

  idecode.io.basic.instruction := Mux(mem.io.stall,
                                        prev_instr,
                                        mem.io.instr_out & Fill(config.XLen, rst)
                                     )
  idecode.io.stall_reg   := pc.io.stall_reg

  // Register File
  register_file.io.decoder.rs1_sel := idecode.io.basic.out.registers.rs1_sel
  register_file.io.decoder.rs2_sel := idecode.io.basic.out.registers.rs2_sel

  // MUX Selections to Operands in ALU
  // Don't read from the forwarding path when operating on PC
  val sel_rs1 = Mux(sel_frw_path_rs1 && idecode.io.basic.out.sel_operand_a =/= 1.U,
                    2.U,
                    idecode.io.basic.out.sel_operand_a)
  alu.io.in.operand_A := MuxLookup(sel_rs1, 0.S,
                                  Array(
                                    0.U -> register_file.io.registers.rs1,
                                    1.U -> ex_pc.asSInt,
                                    2.U -> write_back
                                  ))

  alu.io.in.operand_B := Mux(sel_frw_path_rs2,
                            write_back,
                             Mux(idecode.io.basic.out.switch_2_imm,
                                 idecode.io.basic.out.immediate,
                                 register_file.io.registers.rs2)
                         )
  alu.io.in.decoder_params <> idecode.io.basic.out.alu

  // Memory Connections
  mem.io.in.data_in := register_file.io.registers.rs2
  mem.io.in.addr    := alu.io.result.asUInt
  mem.io.decode     <> idecode.io.basic.out.mem

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
    rsd_sel_wb := idecode.io.basic.out.registers.rsd_sel
    we_wb      := idecode.io.basic.out.registers.we
    alu_res_wb := alu.io.result
    sel_rf_wb  := idecode.io.basic.out.sel_rf_wb
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
  sel_frw_path_rs1 := rsd_sel_wb === idecode.io.basic.out.registers.rs1_sel &&
    rsd_sel_wb =/= 0.U && we_wb
  sel_frw_path_rs2 := rsd_sel_wb === idecode.io.basic.out.registers.rs2_sel &&
    rsd_sel_wb =/= 0.U && we_wb

  ///////////////////////////////////////////////////////////////////
  // Debug Stuff
  ///////////////////////////////////////////////////////////////////

  io.trap := idecode.io.basic.out.trap

  // Simulation ends when program detects a write of 0xdead0000 to R13
  if (config.sim) {
    val success = mem.io.instr_out === "h_dead_0737".U

    register_file.io.success.getOrElse(false.B) := (success | idecode.io.basic.out.trap) &
                                                    RegNext(~io.load.we) & ~io.load.we
    pc.io.success.getOrElse(false.B)            := (success | idecode.io.basic.out.trap) &
                                                    RegNext(~io.load.we) & ~io.load.we
    io.success                                  := RegNext(success)
  } else {
    io.success := false.B
  }

  if (config.sim && config.verbose >= 2) {
    // Debug
    // Stole this from Sodor
    // https://github.com/ucb-bar/riscv-sodor/blob/master/src/rv32_1stage/dpath.scala#L196
    printf("ALU, RF and Mem\n")
    printf("EX PC=[0x%x], Op1=[0x%x] Op2=[0x%x] W[%b, %d = 0x%x] Mem[%b: R:0x%x W:0x%x] DASM(0x%x)\n"
            , ex_pc
            , alu.io.in.operand_A
            , alu.io.in.operand_B
            , idecode.io.basic.out.registers.we
            , idecode.io.basic.out.registers.rsd_sel
            , register_file.io.rsd_value
            , idecode.io.basic.out.sel_rf_wb
            , mem.io.data_out
            , mem.io.in.data_in
            , mem.io.instr_out)

    printf("Forwarding Paths\n")
    printf("FRW Path RS1=[%b], FRW Path RS2=[%b]\n", sel_frw_path_rs1, sel_frw_path_rs2)
  }
}

object Adept extends App {
  val (config, firrtlArgs) = AdeptConfig(args)
  chisel3.Driver.execute(firrtlArgs, () => new Adept(config))
}
