// See LICENSE for license details.
package adept.config

import chisel3._

/* Currently only supports the RV32I Base Instruction Set
 */
class AdeptConfig(simulation: Boolean = false, verboseMode: Int = 0) {
  // Should the memory be simulated? You can find more memory details in the
  // memory.scala file.
  val sim = simulation
  val verbose = verboseMode

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

/*
 *  Parse command line arguments and generate an Adept configuration
 */
object AdeptConfig {
  final val USAGE = StringContext.treatEscapes("""Usage:
    #\t -s | --simulation
    #\t -f | --fpga       => Generate an Adept model to be used in simulation or an FPGA
    #\t -v | -vv          => Increase verbosity of the simulation
    #\t                      (only valid if the simulation flag is set)
    #\t -h | --help       => Prints this message
    #""".stripMargin('#'))

  def printUsage = {
    println("Adept v" + getClass.getPackage.getImplementationVersion)
    print(USAGE)
  }

  def apply(args: Array[String]) : (AdeptConfig, Array[String]) = {
    var simulation = false
    var verboseMode = 0

    var firrtlArgs = Array[String]()

    // Recall that by looping through the args this way the arg which
    // appears last will be used in the configuration.
    for (arg <- args) {
      arg match {
        case "--simulation" | "-s" | "-f" | "--fpga" => simulation = true
        // Our help command takes precedence over FIRRTL
        case "--help" | "-h" => {
          printUsage
          System.exit(0)
        }
        case "-v" => verboseMode = 1
        case "-vv" => verboseMode = 2
        // Any other arg which we haven't specified is going to be thrown
        // in FIRRTL's way and we'll let it handle it.
        case _ => firrtlArgs = firrtlArgs :+ arg
      }
    }

    return (new AdeptConfig(simulation, verboseMode), firrtlArgs)
  }

}
