package adept.pc

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import adept.config.AdeptConfig
import adept.pc.tests._

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
class OldPcTester(e: Pc) extends PeekPokeTester(e) {
  val JAL     = Integer.parseInt("1101111", 2)
  val JALR    = Integer.parseInt("1100111", 2)
  val Cond_Br = Integer.parseInt("1100011", 2)
  val BR_EQ   = 0
  val BR_NE   = 1
  val BR_LT   = 4
  val BR_LTU  = 6
  val BR_GE   = 5
  val BR_GEU  = 7
  val BR      = Array(Cond_Br, JALR, JAL)
  val MAX_Int_32B = 0x7FFFFFFF // in scala this is the maximum 32 bits positive integer
  val MAX_Int_7B  = 0x7F  // max integer of 7 bits
  val MAX_Int_12B = 0xFFF // max integer of 12 bits

  var res_reg                                     = BigInt(0x10000000) // start memory address
  var pc_in, res                                  = BigInt(0)
  var flags, mem_stall, mem_en, stall, mem_en_del = false
  var opcode, offset, st, k, br_type, step_in     = 0

  for (i <- 0 until 100) {
    // To increase the number of jumps the next "if" condition chooses
    // between a random jump or a random integer for the opcode
    // every 5 iterations
    k += 1
    if (k == 5) {
      opcode = BR(rnd.nextInt(3))
      k = 0
    } else {
      opcode = rnd.nextInt(MAX_Int_7B)
    }

    br_type   = rnd.nextInt(7)
    flags     = rnd.nextInt(2) == 1
    mem_en    = rnd.nextInt(2) == 1
    mem_stall = rnd.nextInt(2) == 1
    step_in   = rnd.nextInt(MAX_Int_32B) & 0xFFFFFFFE // logic to force LSB to '0'
    offset    = rnd.nextInt(MAX_Int_12B) // random integer to a maximum of power(2,13)-1 (12 bits) as used in Adept architecture


    // Opcode verification
    opcode match {
      case JALR => {
        res = step_in
        st  = 1
      }
      case JAL => {
        res = pc_in + offset
        st  = 1
      }
      case Cond_Br => {
        br_type match {
          case BR_NE | BR_LT | BR_LTU if flags => {
            res = pc_in + offset
            st  = 1
          }
          case BR_EQ | BR_GE | BR_GEU if !flags => {
            res = pc_in + offset
            st  = 1
          }
          case _ => res = res_reg + 4
        }
      }
      case _ => {
        res = res_reg + 4
      }
    }

    // Logic to detect unsigned integer wraparound
    // in this case is not very useful because a limited offset is used and the test limit is not big
    // but keeping in mind that offset is randomly generated, this can happen
    val UInt_limit = BigInt(MAX_Int_32B) * 2 + 2
    if (res > UInt_limit){
      res = res - UInt_limit
    }

    poke(e.io.br_flags, flags)
    poke(e.io.in_opcode, opcode)
    poke(e.io.br_func, br_type)
    poke(e.io.br_step, step_in)
    poke(e.io.br_offset, offset)
    poke(e.io.pc_in, pc_in)
    poke(e.io.stall, mem_stall)
    poke(e.io.mem_en, mem_en)

    step(1)

    val not_stall = !stall & !mem_stall & (mem_en_del ^ !mem_en)

    stall = st == 1 // branch stall variable

    if (not_stall) {
      res_reg = res
    }

    expect(e.io.pc_out, res_reg)
    expect(e.io.stall_reg, stall)

    pc_in = res_reg
    st = 0
    mem_en_del = mem_en
  }
}

class PcUnitTester(c: Pc) extends PeekPokeTester(c) {
  new BR_EQ(c)
}

class PcTester extends ChiselFlatSpec {
  // Generate configuration
  val config = new AdeptConfig
  val branch_config = new BranchOpConstants

  private val backendNames = Array("firrtl", "verilator")

  for ( backendName <- backendNames ) {
    "PC" should s"test BEQ operations (with $backendName)" in {
      Driver(() => new Pc(config, branch_config), backendName) {
        c => new BR_EQ(c)
      } should be (true)
    }
    "PC" should s"test BNE operations (with $backendName)" in {
      Driver(() => new Pc(config, branch_config), backendName) {
        c => new BR_NE(c)
      } should be (true)
    }
  }
}
