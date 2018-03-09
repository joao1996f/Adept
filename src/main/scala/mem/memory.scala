package adept.mem

import adept.config.AdeptConfig

class MemIO(val config: AdeptConfig) extends Bundle {
  // Inputs
  val data_in = Input(SInt(config.XLen.W))
  val addr_w  = Input(UInt(config.XLen.W))
  val addr_r  = Input(UInt(config.XLen.W))
  val we      = Input(Bool())

  override def cloneType: this.type = {
    new MemIO(config).asInstanceOf[this.type];
  }
}

class Memory(config: AdeptConfig) extends Module {
  val io = IO(new Bundle {
                val in = new MemIO(config)

                val data_out = Output(SInt(config.XLen.W))
              })

  // Because the core is made up of one stage, the memory needs to return a
  // result in the same clock cycle. The memory uses 8MB.
  val my_mem = Mem(gen, 1 << 21)
  val addr_w = io.in.addr_w.asUInt
  val addr_r = io.in.addr_r.asUInt

  when (io.in.we) {
    my_mem.write(addr_w(9, 0), io.in.data_in)
    io.data_out := 0.U
  } .otherwise {
    io.data_out := my_mem.read(addr_r(9, 0))
  }
}
