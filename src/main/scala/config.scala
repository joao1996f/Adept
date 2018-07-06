// See LICENSE for license details.
package adept.config

import chisel3._

/* Currently only supports the RV32I Base Instruction Set
 */
class AdeptConfig(simulation: Boolean = true, verboseMode: Int = 0) {
  // Should the memory be simulated? You can find more memory details in the
  // memory.scala file.
  val sim = simulation
  val verbose = verboseMode

  require(verbose < 3 && verbose >= 0, "Verbosity level can't be negative or greater than 2")

  // ISA length
  val XLen = 32

  // Number of registers
  val n_registers = 32

  require(XLen == 32, "We only support 32-bit instructions")

  // Register Length
  val rs_len = 5

  // Immediates
  // R-Type
  val imm = 12

  val funct = 3

  val op_code = 7
}
