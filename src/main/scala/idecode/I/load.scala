package adept.idecode

import chisel3._

import adept.config.AdeptConfig

class LoadControlSignals(override val config: AdeptConfig,
                         instruction: UInt, decoder_out: InstructionDecoderOutput)
    extends InstructionControlSignals(config, instruction, decoder_out) {

  op_code := op_codes.Loads

  def generateControlSignals(config: AdeptConfig, instruction: UInt) = {
    io.registers.we      := true.B
    io.registers.rsd_sel := instruction(11, 7)
    io.registers.rs1_sel := instruction(19, 15)

    io.alu.switch_2_imm := true.B
    io.alu.imm          := instruction(31, 20).asSInt
    io.alu.op           := 0.U // Add to create relative address
    io.alu.op_code      := op_codes.Loads

    io.sel_rf_wb     := 1.U // Selects Memory to write to the Register File
    io.sel_operand_a := 0.U // Select RS1 to be an input of the ALU

    io.mem.en := true.B
    io.mem.op := instruction(14, 12)
  }

}
