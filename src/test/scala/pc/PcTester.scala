package adept.pc

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import scala.math._
import adept.config.AdeptConfig
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
 * generated randomly with addition of other input variables also
 * randomly generated.
 *
 * The intent is to emulate the test as close to reality as possible
*/
class PcUnitTester(e: Pc) extends PeekPokeTester(e) {
  val JAL     = Integer.parseInt ("1101111", 2)
  val JALR    = Integer.parseInt ("1100111", 2)
  val Cond_Br = Integer.parseInt ("1100011", 2)
  // Next define neded to syncronize test and component
  private def JALr (){
    poke(e.io.br_flags, false)
    poke(e.io.in_opcode, Integer.parseInt ("0001100111", 2))
    poke(e.io.br_step, 0)
    poke(e.io.br_offset, 0)
    poke(e.io.pc_in, 0)
    poke(e.io.mem_en, false)
    poke(e.io.mem_stall, false)
    step(1)
    expect(e.io.pc_out, 0)
    expect(e.io.stall_reg, true)
  }
  var res, pc_in, res_reg                         = BigInt(0)
  var flags, mem_stall, mem_en, stall, mem_en_del = false
  var opcode_in, step_in, offset, st              = 0
  for (i <- 0 until 100){
    opcode_in = rnd.nextInt (pow (2, 10).toInt) // random opcode not to be bigger than a power(2, 10) (10 bits)
    flags     = rnd.nextInt (2)==1 // random Boolean
    mem_en    = rnd.nextInt (2)==1 // random Boolean
    mem_stall = rnd.nextInt (2)==1 // random Boolean
    step_in   = rnd.nextInt (pow (2, 31).toInt)
    offset    = rnd.nextInt (pow (2, 13).toInt) // random integer to a maximum of power(2,13) as used in Adept architecture

    /* As scala language truncates the binary representation of a number to the first high most significant bit(MSB)
     * the binary string manipulation of the variable turns more complicated so to facilitate, a '1' it is added
     * to 11th position of a 10 bit number that "opcode_in" is supposed to be, then manipulate the string of bits
     * so it is truncated and it doesn't influence the result.
     */

    val opcode_extend = opcode_in | Integer.parseInt ("10000000000", 2) // extension explained in comment above
    val opcode_string = opcode_extend.toBinaryString // transformation to a string of bits
    // selection from the privious string the bits needed
    val br_fun_str    = opcode_string.slice (opcode_string.length-10, opcode_string.length-7)

    val br_type = Integer.parseInt (br_fun_str, 2) // transformation of the privious selected bits into integer
    val opcode  = opcode_in & Integer.parseInt ("1111111", 2) // extraction of 7 bits opcode

    // In order to star form '0' and the Pc module to be in sync with the tester file
    // a JALr branch is forced in the next condition with respective variables
    if(i==0){
      mem_en    = false
      mem_stall = false
      res       = 0
      JALr ()
      stall     = true
      res_reg   = res
    } else {

      // Next condition is used to evaluate if the opcode is a branch
      if (opcode == JAL || opcode == JALR || opcode == Cond_Br){
        // Confirming it is a branch
        // next is finding which specific branch it is
        if (opcode == JALR){
          res = step_in
          st  = 1
        } else if (opcode == JAL){
          res = pc_in+ (offset/4).toInt
          st  = 1
        } else {
          br_type match {
            case 0 => {
              if (!flags){
                res = pc_in+ (offset/4).toInt
                st  = 1
              }else {
                res = res_reg +1
              }
            }
            case 1 => {
              if (flags ){
                res = pc_in+ (offset/4).toInt
                st  = 1
              }else {
                res = res_reg +1
              }
            }
            case 4 => {
              if (flags){
                res = pc_in+ (offset/4).toInt
                st  = 1
              }else {
                res = res_reg +1
              }
            }
            case 5 => {
              if (!flags ){
                res = pc_in+ (offset/4).toInt
                st  = 1
              }else {
                res = res_reg +1
              }
            }
            case 6 => {
              if (flags){
                res = pc_in+ (offset/4).toInt
                st  = 1
              }else {
                res = res_reg +1
              }
            }
            case 7 => {
              if (!flags){
                res = pc_in+ (offset/4).toInt
                st  = 1
              }else {
                res = res_reg +1
              }
            }
            case _ => {
              res = res_reg +1
            }
          }
        }
      } else {
        res = res_reg +1
      }

      // logic to detect unsigned integer wraparound
      // in this case is not very useful because a limited offset is used and the test limit is not big
      // but keeping in mind that offset is randomly generated, this can happen
      val a = pow(2,31)
      val b = BigInt(a.toInt) * 2 + 2
      if (res > b){
        res = res - b
      }

      poke(e.io.br_flags, flags)
      poke(e.io.in_opcode, opcode_in)
      poke(e.io.br_step, step_in)
      poke(e.io.br_offset, offset)
      poke(e.io.pc_in, pc_in)
      poke(e.io.mem_stall, mem_stall)
      poke(e.io.mem_en, mem_en)

      step(1)
      // Adept stall logic. Including memory stall
      val not_stall = !stall & !mem_stall & (mem_en_del ^ !mem_en) // it's "true" while there are no stalls; "false" otherwise
      stall = st == 1 // branch stall variable
      if (not_stall){
        res_reg = res
      }
      expect(e.io.pc_out, res_reg)
      expect(e.io.stall_reg, stall)
    }
    pc_in = res_reg
    st = 0
    mem_en_del = mem_en
  }
}

class PcTester extends ChiselFlatSpec {
  // Generate configuration
  val config = new AdeptConfig
  val branch_config = new BranchOpConstants

  private val backendNames = Array("firrtl", "verilator")

  for ( backendName <- backendNames ) {
    "PC" should s"do stuff (with $backendName)" in {
      Driver(() => new Pc(config, branch_config), backendName) {
        e => new PcUnitTester(e)
      } should be (true)
    }
  }
}
