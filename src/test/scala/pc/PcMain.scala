package adept.pc

import chisel3._

import adept.config.AdeptConfig
import adept.pc.BranchOpConstants
import adept.test.common.Common

object PcMain extends App {
  val config= new AdeptConfig
  val b = new BranchOpConstants

  val parseArgs = Common(args)

  iotesters.Driver.execute(parseArgs.firrtlArgs, () => new Pc(config, b)) {
    c => new PcUnitTester(c)
  }
}

object PcRepl extends App {
  val config= new AdeptConfig
  val b = new BranchOpConstants
  iotesters.Driver.executeFirrtlRepl(args, () => new Pc(config, b))
}
