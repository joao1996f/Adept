package adept.mem

import chisel3._
import chisel3.util.Counter

import adept.config.AdeptConfig

class CacheIO(config: AdeptConfig) extends Bundle {
  // Handshake Protocol
  val valid = Input(Bool())
  val ready = Output(Bool())

  // Cache Datapath signals
  val addr = Input(UInt(config.XLen.W))
  val mask = Input(Vec(4, Bool()))
  val we   = Input(Bool())
  val re   = Input(Bool())

  val data_in  = Input(Vec(4, UInt(8.W)))
  val data_out = Output(Vec(4, UInt(8.W)))
}

class Cache(config: AdeptConfig) extends BlackBox {
  val io = IO(new CacheIO(config))
}

class CacheSim(config: AdeptConfig) extends Module {
  val io = IO(new CacheIO(config))

  // Simulation Only
  val my_mem  = SyncReadMem(Vec(4, UInt(8.W)), 1 << 15)
  val valid   = RegInit(false.B)
  val ready   = RegInit(false.B)
  val counter = Counter(55)

  when (io.we && valid && ready) {
    my_mem.write(io.addr, io.data_in, io.mask)
  }

  when (io.re && valid && ready) {
    io.data_out := my_mem.read(io.addr)
  } .otherwise {
    io.data_out := Vec(0.U, 0.U, 0.U, 0.U)
  }

  valid := io.valid
  io.ready := ready

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
