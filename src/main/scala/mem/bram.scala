package adept.mem

import chisel3._
import chisel3.experimental._
import chisel3.util._

class SimBRAM extends BlackBox(Map(
                              "MEM_SIZE" -> 2097152, // 1 << 21
                              "XILINX" -> 1,
                              "ALTERA" -> 0
                            )) with HasBlackBoxResource {
  val io = IO(new Bundle {
                   val clock = Input(Clock())

                   // Write Mask
                   val io_mask_0 = Input(Bool())
                   val io_mask_1 = Input(Bool())
                   val io_mask_2 = Input(Bool())
                   val io_mask_3 = Input(Bool())

                   // Port Enables
                   val io_en_A = Input(Bool())
                   val io_en_B = Input(Bool())

                   // Data address
                   val io_data_addr = Input(UInt(32.W))
                   val io_data_we = Input(Bool())

                   // Data Write Bus
                   val io_data_in_0 = Input(UInt(8.W))
                   val io_data_in_1 = Input(UInt(8.W))
                   val io_data_in_2 = Input(UInt(8.W))
                   val io_data_in_3 = Input(UInt(8.W))

                   // Data Read Bus
                   val io_data_out_0 = Output(UInt(8.W))
                   val io_data_out_1 = Output(UInt(8.W))
                   val io_data_out_2 = Output(UInt(8.W))
                   val io_data_out_3 = Output(UInt(8.W))

                   // Program Loading Bus
                   val io_load_data_in_0 = Input(UInt(8.W))
                   val io_load_data_in_1 = Input(UInt(8.W))
                   val io_load_data_in_2 = Input(UInt(8.W))
                   val io_load_data_in_3 = Input(UInt(8.W))

                   val io_load_we = Input(Bool())

                   // Instruction Read Bus
                   val io_instr_addr = Input(UInt(32.W))

                   val io_instr_out_0 = Output(UInt(8.W))
                   val io_instr_out_1 = Output(UInt(8.W))
                   val io_instr_out_2 = Output(UInt(8.W))
                   val io_instr_out_3 = Output(UInt(8.W))
                  })

  // Verilog Model
  setResource("/SimBRAM.v")
}
