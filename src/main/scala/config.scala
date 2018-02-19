package adept.config

import chisel3._

/* Currently only support the RV32I Base Instruction Set
 */
trait AdeptConfig {
  // ISA lenght
  val XLen = 32

  require(XLen == 32, "We only support 32-bit instructions")

  // Register Length
  val rs_len = if (XLen == 32) {
    5
  } else {
    6
  }

  // Immediates
  // R-Type
  val imm = if (XLen == 32) {
    12
  }

  val funct = if (XLen == 32) {
    3
  }

  val op_code = if (XLen == 32) {
    7
  }
}
