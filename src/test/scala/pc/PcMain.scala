package adept.pc

import chisel3._

import adept.config.AdeptConfig
import adept.test.common.Common

object PcMain extends App {
  val config= new AdeptConfig
  val branchConsts = new BranchOpConstants

  val parseArgs = Common(args)

  iotesters.Driver.execute(parseArgs.firrtlArgs, () => new Pc(config, branchConsts)) {
    c => new PcUnitTester(c)
  }
}

object PcRepl extends App {
  val config= new AdeptConfig
  val b = new BranchOpConstants
  iotesters.Driver.executeFirrtlRepl(args, () => new Pc(config, b))
}
