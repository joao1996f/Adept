package adept.core

import scala.io.Source

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.config.AdeptConfig

class AdeptUnitTester(c: Adept, programFileName: String) extends PeekPokeTester(c) {
  // Load program while core is in reset
  var program = Array[BigInt]()
  // Read from file the program and transform the hexadecimal string into BigInt
  val programFile = Source.fromFile(programFileName)
  for(lines <- programFile.getLines){
    program = program :+ BigInt(lines.substring(1, lines.length), 16)
  }
  programFile.close

  for ((data, addr) <- program.zipWithIndex) {
    poke(c.io.we, true)
    poke(c.io.data_in, data)
    poke(c.io.addr_w, addr)
    step(1)
  }
  // Lower reset and write_enable. Core should start processing
  poke(c.io.we, false)

  reset(4)

  // Wait for success
  //while(peek(c.io.success) == 0) {
    step(800)
  //}

}
