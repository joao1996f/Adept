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

  private def splitData(data: BigInt) : Array[BigInt] = {
    var newData = Array[BigInt]()

    for (i <- 0 until 4) {
      newData = newData :+ (((0x000000ff << (i * 8)) & data) >> (i * 8))
    }

    return newData
  }

  for ((data, addr) <- program.zipWithIndex) {
    poke(c.io.load.we, true)

    val newData = splitData(data)
    for(i <- 0 until 4){
      poke(c.io.load.data_in(i), newData(i))
    }

    poke(c.io.load.addr_w, addr * 4)
    step(1)
  }
  // Lower reset and write_enable. Core should start processing
  poke(c.io.load.we, false)

  reset(4)

  // Wait for success
  //while(peek(c.io.success) == 0) {
    step(1500)
  //}

}
