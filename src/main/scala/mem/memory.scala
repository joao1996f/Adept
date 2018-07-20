// See LICENSE for license details.
package adept.mem

import chisel3._
import chisel3.util._

import adept.config.AdeptConfig
import adept.decoder.OpCodes

class MemIO(val config: AdeptConfig) extends Bundle {
  // Inputs
  val data_in = Input(SInt(config.XLen.W))
  val addr    = Input(UInt(config.XLen.W))

  override def cloneType: this.type = {
    new MemIO(config).asInstanceOf[this.type]
  }
}

class MemLoadIO(config: AdeptConfig) extends Bundle {
  // Inputs
  val data_in = Input(Vec(4, UInt(8.W)))
  val addr_w  = Input(UInt(config.XLen.W))
  val we      = Input(Bool())

  override def cloneType: this.type = {
    new MemLoadIO(config).asInstanceOf[this.type]
  }
}

class DecoderMemIO(val config: AdeptConfig) extends Bundle {
  val op = UInt(3.W)
  val we = Bool()
  val en = Bool()

  override def cloneType: this.type = {
    new DecoderMemIO(config).asInstanceOf[this.type]
  }

  def setDefaults = {
    op := DontCare
    we := false.B
    en := false.B
  }
}

final object MemOps {
  val lb :: lh :: lw :: lbu :: lhu :: sb :: sh :: sw :: Nil = Enum(8)

  private val op_codes = new OpCodes

  def getMemOp(funct: UInt, op_code: UInt) : UInt = {
    require(funct.getWidth == 3, "ALU Operations are only 3 bits wide")
    require(op_code.getWidth == 7, "OP Codes are only 7 bits wide")

    val result = WireInit(0.U(3.W))

    when (op_code === op_codes.Loads) {
      when (funct === 0.U) {
        result := lb
      } .elsewhen (funct === 1.U) {
        result := lh
      } .elsewhen (funct === 2.U) {
        result := lw
      } .elsewhen (funct === 4.U) {
        result := lbu
      } .otherwise {
        result := lhu
      }
    } .otherwise {
      when (funct === 0.U) {
        result := sb
      } .elsewhen (funct === 1.U) {
        result := sh
      } .otherwise {
        result := sw
      }
    }

    return result
  }
}

class Memory(config: AdeptConfig) extends Module {
  val io = IO(new Bundle {
                // Program Load
                val load     = new MemLoadIO(config)

                // Data R/W Ports
                val in       = new MemIO(config)
                val decode   = Input(new DecoderMemIO(config))
                val data_out = Output(SInt(config.XLen.W))

                // Instruction Read Port
                val pc_in     = Input(UInt(config.XLen.W))
                val instr_out = Output(UInt(config.XLen.W))

                val stall     = Output(Bool())
              })

  private def buildWriteData(data_in: UInt, op: UInt, byte_sel_write: UInt) : Vec[UInt] = {
    val new_data = WireInit(VecInit(Seq.fill(config.XLen/8)(0.U((config.XLen/4).W))))
    val mem_ops  = MemOps

    when (op === mem_ops.sb) {
      new_data(byte_sel_write)       := data_in(7, 0)
      new_data(byte_sel_write + 1.U) := 0.U
      new_data(byte_sel_write + 2.U) := 0.U
      new_data(byte_sel_write + 3.U) := 0.U
    } .elsewhen (op === mem_ops.sh) {
      new_data(byte_sel_write)       := data_in(7, 0)
      new_data(byte_sel_write + 1.U) := data_in(15, 8)
      new_data(byte_sel_write + 2.U) := 0.U
      new_data(byte_sel_write + 3.U) := 0.U
    } .otherwise {
      new_data(byte_sel_write)       := data_in(7, 0)
      new_data(byte_sel_write + 1.U) := data_in(15, 8)
      new_data(byte_sel_write + 2.U) := data_in(23, 16)
      new_data(byte_sel_write + 3.U) := data_in(31, 24)
    }

    return new_data
  }

  private def buildWriteMask(op: UInt, byte_sel_write: UInt) : UInt = {
    val mask    = Wire(UInt(4.W))
    val mem_ops = MemOps

    mask := Mux(op === mem_ops.sb,
                1.U << byte_sel_write,
                Mux(op === mem_ops.sh,
                    3.U << byte_sel_write,
                    15.U << byte_sel_write)
            )

    return mask
  }

  private def buildReadData(op: UInt, byte_sel_read: UInt, read_port: Vec[UInt]) : UInt = {
    val byte_sel_read_reg = RegNext(byte_sel_read)
    val mem_ops = MemOps

    return MuxLookup(op, op, Array(
                // Load Byte (8 bits)
                mem_ops.lb  -> Cat(Fill(config.XLen - 8, read_port(byte_sel_read_reg)(7)), read_port(byte_sel_read_reg)),
                // Load Half (16 bits)
                mem_ops.lh  -> Cat(Fill(config.XLen - 16, read_port(byte_sel_read_reg + 1.U)(7)), read_port(byte_sel_read_reg + 1.U), read_port(byte_sel_read_reg)),
                // Load Word (32 bits)
                mem_ops.lw  -> Cat(read_port(3), read_port(2), read_port(1), read_port(0)),
                // Load Byte Unsigned (8 bits)
                mem_ops.lbu -> Cat(0.U, read_port(byte_sel_read_reg)),
                // Load Half Unsigned (16 bits)
                mem_ops.lhu -> Cat(0.U, read_port(byte_sel_read_reg + 1.U), read_port(byte_sel_read_reg))
              ))
  }

  // val my_mem = if (config.sim_mem) {
    // The memory in simulation uses 131kB. You can also turn this option on
    // when operating on an FPGA. Know that this will instantiate BRAMs, and
    // these will only be available to the core. Feel free to increase the size
    // of the memory.
    val my_mem = Module(new CacheSim(config))
  // } else {
    // Create interface for ASIC external memory
    // Module(new Cache(config))
  // }

  val read_port = WireInit(VecInit(Seq.fill(config.XLen/8)(0.U((config.XLen/4).W))))
  val addr = io.in.addr >> 2

  // Pass PC to memory
  my_mem.io.cache.pc_in := io.pc_in

  // Connect Program Loading interface
  my_mem.io.load <> io.load

  // Pass address, read and write enable
  my_mem.io.cache.addr := addr
  my_mem.io.cache.we   := io.decode.we

  when (io.decode.we && io.decode.en) {
    // Write Port
    my_mem.io.cache.data_in := buildWriteData(io.in.data_in.asUInt, io.decode.op, io.in.addr(1, 0).asUInt)
    my_mem.io.cache.mask    := buildWriteMask(io.decode.op, io.in.addr(1, 0).asUInt).toBools
  } .otherwise {
    // Read Port
    my_mem.io.cache.data_in := VecInit(0.U, 0.U, 0.U, 0.U)
    my_mem.io.cache.mask    := VecInit(false.B, false.B, false.B, false.B)
    read_port               := my_mem.io.cache.data_out
  }

  // Stall logic
  // Handshake protocol with Cache
  val ready = RegInit(false.B)
  val valid = RegInit(false.B)
  val stall = RegInit(false.B)

  ready                 := my_mem.io.cache.ready
  my_mem.io.cache.valid := valid

  when (io.decode.en) {
    valid := true.B
    stall := true.B
  } .otherwise {
    valid := false.B
    stall := false.B
  }

  when (ready && valid && io.decode.en) {
    stall := false.B
    valid := false.B
  }

  ////////////////////////////////////////////////////////////////////////////////
  // Outputs
  ////////////////////////////////////////////////////////////////////////////////
  io.stall := stall

  // Data Read Port
  io.data_out := buildReadData(io.decode.op, io.in.addr(1, 0).asUInt, read_port).asSInt

  // Instruction Read Port
  io.instr_out := my_mem.io.cache.pc_out
}
