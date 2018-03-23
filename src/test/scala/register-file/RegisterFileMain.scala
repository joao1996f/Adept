package adept.registerfile

import chisel3._

import adept.config.AdeptConfig
import adept.test.common.Common

object RegisterFileMain extends App {
  private val config = new AdeptConfig

  val parseArgs = Common(args)

  iotesters.Driver.execute(parseArgs.firrtlArgs, () => new RegisterFile(config)) {
    c => new RegisterFileUnitTester(c)
  }
}

object RegisterFileRepl extends App {
  private val config = new AdeptConfig

  iotesters.Driver.executeFirrtlRepl(args, () => new RegisterFile(config))
}
