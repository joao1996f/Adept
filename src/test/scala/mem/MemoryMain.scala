package adept.mem

import chisel3._

import adept.config.AdeptConfig
import adept.test.common.Common

object MemoryMain extends App {
  private val config = new AdeptConfig

  val parseArgs = Common(args)

  iotesters.Driver.execute(parseArgs.firrtlArgs, () => new Memory(config)) {
    c => new MemoryUnitTester(c, config)
  }
}

object MemoryRepl extends App {
  private val config = new AdeptConfig

  iotesters.Driver.executeFirrtlRepl(args, () => new Memory(config))
}
