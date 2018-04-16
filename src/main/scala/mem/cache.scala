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
  val mask  = Input(Vec(4, Bool()))
  val we    = Input(Bool())
  val re    = Input(Bool())
  val pc_en = Input(Bool())

  val data_in  = Input(Vec(4, UInt(8.W)))
  val data_out = Output(Vec(4, UInt(8.W)))
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
  val my_mem  = SyncReadMem(Vec(4, UInt(8.W)), 1 << 15)
  val valid   = RegInit(false.B)
  val ready   = RegInit(false.B)
  val counter = Counter(55)

  // Program loading write port
  when (io.load.we) {
    my_mem.write(io.load.addr_w, io.load.data_in)
  }

  // Data read and write ports
  when (io.cache.we && valid && ready) {
    my_mem.write(io.cache.addr, io.cache.data_in, io.cache.mask)
  }

  when (io.cache.re && valid && ready) {
    io.cache.data_out := my_mem.read(io.cache.addr)
  } .otherwise {
    io.cache.data_out := Vec(0.U, 0.U, 0.U, 0.U)
  }

  // Ignore stalling when the PC is reading instructions from memory. This isn't
  // realistic but it's ok for sim.
  when (io.cache.pc_en) {
    val read_pc = my_mem.read(io.cache.pc_in)
    io.cache.pc_out := Cat(read_pc(3), read_pc(2), read_pc(1), read_pc(0))
  } .otherwise {
    io.cache.pc_out := 0.U
  }

  // Handshake logic
  valid := io.cache.valid
  io.cache.ready := ready

  when (valid) {
    // printf("Counter (Cache): %d", counter.value)
    counter.inc()
  }

  when (counter.value === 54.U) {
    ready := true.B
  } .otherwise {
    ready := false.B
  }
}
