// See LICENSE for license details.
package adept.mem

import chisel3._
import chisel3.util.{Counter, Cat}

import adept.config.AdeptConfig
import adept.core.MemLoadIO

class CacheIO(config: AdeptConfig) extends Bundle {
  // Handshake Protocol
  val valid = Input(Bool())
  val ready = Output(Bool())

  // Cache Datapath signals
  val addr  = Input(UInt(config.XLen.W))
  val mask  = Input(Vec(config.XLen/8, Bool()))
  val we    = Input(Bool())

  val data_in  = Input(Vec(config.XLen/8, UInt((config.XLen/4).W)))
  val data_out = Output(Vec(config.XLen/8, UInt((config.XLen/4).W)))
  val pc_in   = Input(UInt(config.XLen.W))
  val pc_out   = Output(UInt(config.XLen.W))
}

class Cache(config: AdeptConfig) extends BlackBox {
  val io = IO(new CacheIO(config))
}

class CacheSim(config: AdeptConfig) extends Module {
  val io = IO(new Bundle {
    val cache = new CacheIO(config)
    val load  = new MemLoadIO(config)
  })

  // Simulation Only
  val valid   = RegInit(false.B)
  val ready   = RegInit(false.B)
  val counter = Counter(55)

  // BRAM model
  val bram = Module(new SimBRAM)
  bram.io.clock := clock

  ////////////////////////////////////////////////////////////////////////////////
  // Data Read and Write Ports
  ////////////////////////////////////////////////////////////////////////////////

  // Mask
  bram.io.io_mask_0 := io.cache.mask(0)
  bram.io.io_mask_1 := io.cache.mask(1)
  bram.io.io_mask_2 := io.cache.mask(2)
  bram.io.io_mask_3 := io.cache.mask(3)

  // Enables
  val enable = valid && ready
  bram.io.io_en_A := RegNext(enable)
  bram.io.io_data_we := io.cache.we

  // Data In
  bram.io.io_data_in_0 := io.cache.data_in(0)
  bram.io.io_data_in_1 := io.cache.data_in(1)
  bram.io.io_data_in_2 := io.cache.data_in(2)
  bram.io.io_data_in_3 := io.cache.data_in(3)

  // Address
  val addr_reg = RegInit(0.U(config.XLen.W))
  when (enable) {
    addr_reg := io.cache.addr
  }
  bram.io.io_data_addr := addr_reg

  // Data Out
  io.cache.data_out(0) := bram.io.io_data_out_0
  io.cache.data_out(1) := bram.io.io_data_out_1
  io.cache.data_out(2) := bram.io.io_data_out_2
  io.cache.data_out(3) := bram.io.io_data_out_3

  ////////////////////////////////////////////////////////////////////////////////
  // Instruction Read and Write Ports
  ////////////////////////////////////////////////////////////////////////////////

  // Instruction In
  bram.io.io_load_data_in_0 := io.load.data_in(0)
  bram.io.io_load_data_in_1 := io.load.data_in(1)
  bram.io.io_load_data_in_2 := io.load.data_in(2)
  bram.io.io_load_data_in_3 := io.load.data_in(3)

  // Enables
  bram.io.io_en_B := true.B // TODO: Don't assume that the PC always has the memory available
  bram.io.io_load_we := io.load.we

  // Address
  bram.io.io_instr_addr := Mux(io.load.we, io.load.addr_w, io.cache.pc_in)

  // Instruction Out
  io.cache.pc_out := Cat(bram.io.io_instr_out_3,
                         bram.io.io_instr_out_2,
                         bram.io.io_instr_out_1,
                         bram.io.io_instr_out_0)

  ////////////////////////////////////////////////////////////////////////////////
  // Handshake logic
  ////////////////////////////////////////////////////////////////////////////////
  valid := io.cache.valid
  io.cache.ready := ready

  when (valid) {
    counter.inc()
  }

  when (counter.value === 54.U) {
    ready := true.B
  } .otherwise {
    ready := false.B
  }
}
