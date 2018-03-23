// See LICENSE for license details.

package adept.core

import chisel3._

import adept.config.AdeptConfig
import adept.test.common.Common

object AdeptMain extends App {
  private val config = new AdeptConfig

  if (args.length < 1) {
    println("You need to provide, at the very least, a test file")
    sys.exit(1)
  }

  val parseArgs = Common(args)

  iotesters.Driver.execute(parseArgs.firrtlArgs, () => new Adept(config)) {
    c => new AdeptUnitTester(c, parseArgs.programFileName)
  }
}

object AdeptRepl extends App {
  private val config = new AdeptConfig

  iotesters.Driver.executeFirrtlRepl(args, () => new Adept(config))
}
