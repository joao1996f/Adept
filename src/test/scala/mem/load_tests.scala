package adept.mem.tests

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import scala.collection.mutable.HashMap

import adept.mem.Memory
import adept.mem.BaseMemory
import adept.config.AdeptConfig

class BaseLoad(c: Memory, config: AdeptConfig) extends BaseMemory(c, config) {
  def setLoadSignals(op: BigInt, addr: Int) {
    poke(c.io.decode.op, op)
    poke(c.io.in.addr, addr)
    poke(c.io.decode.en, true)
    poke(c.io.decode.we, false)
  }

  def getFinalRead(addr: Int, opType: BigInt) : (Int, Boolean) = {
    val lsbs = addr & 0x00000003
    val masked_addr = addr >>> 2

    val bitMask = if (opType == mem_ops.lb.litValue() || opType == mem_ops.lbu.litValue()) {
      0x000000ff << (8 * lsbs)
    } else if ((opType == mem_ops.lh.litValue() || opType == mem_ops.lhu.litValue()) && lsbs < 3) {
      0x0000ffff << (8 * lsbs)
    } else if (opType == mem_ops.lw.litValue() && lsbs == 0) {
      0xffffffff
    } else {
      // TODO: Memory should throw a trap for an ilegal memory op
      0x00000000
    }

    val result = if (lsbs < 4 && bitMask != 0) {
      ((mem_img(masked_addr) & bitMask) >>> (8 * lsbs), true)
    } else {
      // TODO: Memory should throw a trap for an ilegal memory access
      (0, false)
    }

    return result
  }

  def getSignExtend(read: Int, opType: BigInt) : Int = {
    val bitMask = if (opType == mem_ops.lb.litValue() || opType == mem_ops.lbu.litValue()) {
      0x00000080
    } else if (opType == mem_ops.lh.litValue() || opType == mem_ops.lhu.litValue()) {
      0x00008000
    } else {
      0x00000000
    }

    val shift = scala.math.log(bitMask) / scala.math.log(2)

    val signExtend = if (((read & bitMask) >>> shift.toInt) == 1) {
      ~(bitMask | (bitMask - 1))
    } else {
      0x00000000
    }

    return signExtend
  }
}

class LoadWord(c: Memory, config: AdeptConfig) extends BaseLoad(c, config) {
  private def LW(addr: Int, mem_img: HashMap[Int, Int]) = {
    setLoadSignals(mem_ops.lw.litValue(), addr)

    // Ignore output while stall is active and advance simulation
    do {
      step(1)
    } while (peek(c.io.stall) == 1)

    val finalRead = getFinalRead(addr, mem_ops.lw.litValue())

    if (finalRead._2) {
      expect(c.io.data_out, finalRead._1)
    }
  }

  for (i <- 0 until 100) {
    val addr = rnd.nextInt(5000)
    LW(addr, mem_img)
  }
}

class LoadHalf(c: Memory, config: AdeptConfig) extends BaseLoad(c, config) {
  private def LH(addr: Int, mem_img: HashMap[Int, Int]) = {
    setLoadSignals(mem_ops.lh.litValue(), addr)

    // Ignore output while stall is active and advance simulation
    do {
      step(1)
    } while (peek(c.io.stall) == 1)

    val finalRead = getFinalRead(addr, mem_ops.lh.litValue())

    if (finalRead._2) {
      val signExtend = getSignExtend(finalRead._1, mem_ops.lh.litValue())
      expect(c.io.data_out, signExtend | finalRead._1)
    }
  }

  for (i <- 0 until 100) {
    val addr = rnd.nextInt(5000)
    LH(addr, mem_img)
  }
}

class LoadByte(c: Memory, config: AdeptConfig) extends BaseLoad(c, config) {
  private def LB(addr: Int, mem_img: HashMap[Int, Int]) = {
    setLoadSignals(mem_ops.lb.litValue(), addr)

    // Ignore output while stall is active and advance simulation
    do {
      step(1)
    } while (peek(c.io.stall) == 1)

    val finalRead = getFinalRead(addr, mem_ops.lb.litValue())

    if (finalRead._2) {
      val signExtend = getSignExtend(finalRead._1, mem_ops.lb.litValue())
      expect(c.io.data_out, signExtend | finalRead._1)
    }
  }

  for (i <- 0 until 100) {
    val addr = rnd.nextInt(5000)
    LB(addr, mem_img)
  }
}

class LoadHalfUnsigned(c: Memory, config: AdeptConfig) extends BaseLoad(c, config) {
  private def LHU(addr: Int, mem_img: HashMap[Int, Int]) = {
    setLoadSignals(mem_ops.lhu.litValue(), addr)

    // Ignore output while stall is active and advance simulation
    do {
      step(1)
    } while (peek(c.io.stall) == 1)

    val finalRead = getFinalRead(addr, mem_ops.lhu.litValue())

    if (finalRead._2) {
      expect(c.io.data_out, finalRead._1)
    }
  }

  for (i <- 0 until 100) {
    val addr = rnd.nextInt(5000)
    LHU(addr, mem_img)
  }
}

class LoadByteUnsigned(c: Memory, config: AdeptConfig) extends BaseLoad(c, config) {
  private def LBU(addr: Int, mem_img: HashMap[Int, Int]) = {
    setLoadSignals(mem_ops.lbu.litValue(), addr)

    // Ignore output while stall is active and advance simulation
    do {
      step(1)
    } while (peek(c.io.stall) == 1)

    val finalRead = getFinalRead(addr, mem_ops.lbu.litValue())

    if (finalRead._2) {
      expect(c.io.data_out, finalRead._1)
    }
  }

  for (i <- 0 until 100) {
    val addr = rnd.nextInt(5000)
    LBU(addr, mem_img)
  }
}
