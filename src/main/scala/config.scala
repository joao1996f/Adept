package adept.config

import chisel3._

/* Currently only supports the RV32I Base Instruction Set
 */
class AdeptConfig {
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
