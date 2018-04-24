// ALU Unit Tester
package adept.alu

import chisel3._

import adept.config.AdeptConfig
import adept.test.common.Common

object ALUMain extends App {
  private val config = new AdeptConfig

  val parseArgs = Common(args)

  iotesters.Driver.execute(parseArgs.firrtlArgs, () => new ALU(config)) {
    c => new ALUUnitTesterAll(c)
  }
}

object ALURepl extends App {
  private val config = new AdeptConfig

  iotesters.Driver.executeFirrtlRepl(args, () => new ALU(config))
}
