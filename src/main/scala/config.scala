package adept.config

import chisel3._

/* Currently only supports the RV32I Base Instruction Set
 */
class AdeptConfig {
  // Should the memory be simulated? You can find more memory details at the
  // memory.scala file.
  val sim_mem = true

  // ISA length
  val XLen = 32

  require(XLen == 32, "We only support 32-bit instructions")

  // Register Length
  val rs_len = 5

  // Immediates
  // R-Type
  val imm = 12

  val funct = 3

  val op_code = 7
}
