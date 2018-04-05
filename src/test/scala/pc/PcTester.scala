
package adept.pc

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

//import scala.math.pow
import scala.math._
import adept.config.AdeptConfig
import adept.pc.BranchOpConstants
/* Opcode          function
 * 1101111 JAL      XXXX
 * 1100111 JALR     000
 * 1100011 BEQ      000  0
 * 1100011 BNE      001  1
 * 1100011 BLT      100  4
 * 1100011 BGE      101  5
 * 1100011 BLTU     110  6
 * 1100011 BGEU     111  7
*/
/*********************************************
 * The test avaluates the detection of a jump from the opcode
*/
class PcUnitTester(e: Pc) extends PeekPokeTester(e) {
  val JAL = Integer.parseInt ("1101111", 2)
  val JALR = Integer.parseInt ("1100111", 2)
  val Cond_Br = Integer.parseInt ("1100011", 2)
  // Next define neded to syncronize test and component
  private def JALr (flags: Boolean, step_in: Int, offset: Int, res: BigInt){
    poke(e.io.br_flags, flags)
    poke(e.io.in_opcode, Integer.parseInt ("0001100111", 2))
    poke(e.io.br_step, step_in)
    poke(e.io.br_offset, offset)
    step(1)
    expect(e.io.pc_out, res)
  }
  var res =BigInt(0)
  var st = false
  for (i <- 0 until 100){
    val opcode_in = rnd.nextInt ((pow (2, 10)).toInt)
    val flags = rnd.nextInt(2)==1 // random Boolean
    val step_in = rnd.nextInt (pow (2, 10).toInt)
    val offset = rnd.nextInt (pow (2,10).toInt) // the division is for the sake of not exceeding the range limit of int too quickly

    val aux1 = opcode_in | Integer.parseInt ("10000000000", 2) // extension to facilitate future operations
    val aux2 = aux1.toBinaryString // transformation to a string of bits
    val aux3 = aux2.slice (aux2.length-10, aux2.length-7) // selection from the privious string the bits needed

    val br_type = Integer.parseInt (aux3, 2) // transformation of the privious selected bits into integer
    val opcode = opcode_in & Integer.parseInt ("1111111", 2) // extraction of 7 bits opcode
    
    if (opcode == JAL || opcode == JALR || opcode == Cond_Br){
      if (opcode == JALR){
        res = step_in
      } else if (opcode == JAL){
        res += offset
      } else {
        br_type match {
          case 0 => {
            if (!flags){
              res += offset
            }else {
              res += 1
            }
          }
          case 1 => {
            if (flags ){
              res += offset
            }else {
              res += 1
            }
          }
          case 4 => {
            if (flags){
              res+= offset
            }else {
              res += 1
            }
          }
          case 5 => {
            if (!flags ){
              res+= offset
            }else {
              res += 1
            }
          }
          case 6 => {
            if (flags){
              res+= offset
            }else {
              res += 1
            }
          }
          case 7 => {
            if (!flags){
              res+= offset
            }else {
              res += 1
            }
          }
          case _ => res += 1
        }
      }
    } else {
      res += 1
    }
    // logic to detect unsigned integer wraparound
    val a = pow(2,31)
    val b = BigInt(a.toInt) * 2 + 2
    if (res > b){
      res = res - b
    }
    // simulation
    if (i==0){
      res= step_in
      // JALr definition is used to initiate pc to a value to be in sync with the test
      JALr (flags, step_in, offset, res)
    println ("result="+res+ " offset="+ offset )
    } else {
    println ("result="+res+ " offset="+ offset )
      poke(e.io.br_flags, flags)
      poke(e.io.in_opcode, opcode_in)
      poke(e.io.br_step, step_in)
      poke(e.io.br_offset, offset)
      step(1)
      expect(e.io.pc_out, res)
    }
  }
}

/**
* This is a trivial example of how to run this Specification
* From within sbt use:
* {{{
* testOnly adept.pc.PcTester -- -z Basic
* }}}
* From a terminal shell use:
* {{{
* sbt 'testOnly adept.pc.PcTester -- -z Basic'
* }}}
*/
class PcTester extends ChiselFlatSpec {
  // Generate Pc configuration
  val config = new AdeptConfig
  val branch = new BranchOpConstants

  private val backendNames = if(firrtl.FileUtils.isCommandAvailable("verilator")) {
    Array("firrtl", "verilator")
  }
  else {
    Array("firrtl")
  }
  for ( backendName <- backendNames ) {
    "Pc" should s"store random data (with $backendName)" in {
      Driver(() => new Pc(config, branch), backendName) {
        e => new PcUnitTester(e)
      } should be (true)
    }
  }

  "Basic test using Driver.execute" should "be used as an alternative way to run specification" in {
    iotesters.Driver.execute(Array(), () => new Pc(config, branch)) {
      e => new PcUnitTester(e)
    } should be (true)
  }

  "using --backend-name verilator" should "be an alternative way to run using verilator" in {
    if(backendNames.contains("verilator")) {
      iotesters.Driver.execute(Array("--backend-name", "verilator"), () => new Pc(config, branch)) {
        e => new PcUnitTester(e)
      } should be(true)
    }
  }

  "running with --is-verbose" should "show more about what's going on in your tester" in {
    iotesters.Driver.execute(Array("--is-verbose"), () => new Pc(config, branch)) {
      e => new PcUnitTester(e)
    } should be(true)
  }

  "running with --fint-write-vcd" should "create a vcd file from your test" in {
    iotesters.Driver.execute(Array("--fint-write-vcd"), () => new Pc(config, branch)) {
      e => new PcUnitTester(e)
    } should be(true)
  }

  "using --help" should s"show the many options available" in {
    iotesters.Driver.execute(Array("--help"), () => new Pc(config, branch)) {
      e => new PcUnitTester(e)
    } should be (true)
  }
}
