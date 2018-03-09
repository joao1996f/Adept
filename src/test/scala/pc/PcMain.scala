package adept.pc

import chisel3._
import adept.config.AdeptConfig
import adept.pc.BranchOpConstants

object PcMain extends App {
  val config= new AdeptConfig
  val b = new BranchOpConstants
  iotesters.Driver.execute(args, () => new Pc(config, b)) {
    c => new PcUnitTester(c)
  }
}

object PcRepl extends App {
  val config= new AdeptConfig
  val b = new BranchOpConstants
  iotesters.Driver.executeFirrtlRepl(args, () => new Pc(config, b))
}
