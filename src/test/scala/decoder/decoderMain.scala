// Decoder Unit Tester
package adept.decoder

import chisel3._

import adept.config.AdeptConfig
import adept.test.common.Common

object DecoderMain extends App {
  private val config = new AdeptConfig

  val parseArgs = Common(args)

  iotesters.Driver.execute(parseArgs.firrtlArgs, () => new InstructionDecoder(config)) {
    c => new DecoderUnitTesterAll(c)
  }
}

object DecoderRepl extends App {
  private val config = new AdeptConfig

  iotesters.Driver.executeFirrtlRepl(args, () => new InstructionDecoder(config))
}
