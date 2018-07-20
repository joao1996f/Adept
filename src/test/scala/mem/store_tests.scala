package adept.mem.tests

import chisel3.iotesters
import chisel3.iotesters.{ChiselFlatSpec, Driver, PeekPokeTester}

import scala.collection.mutable.HashMap

import adept.mem.Memory
import adept.mem.BaseMemory
import adept.config.AdeptConfig

class BaseStore(c: Memory, config: AdeptConfig) extends BaseLoad(c, config) {
  def setWriteSignals(op: BigInt, addr: Int, data_in: BigInt) {
    poke(c.io.in.addr, addr)
    poke(c.io.in.data_in, data_in)
    poke(c.io.decode.op, op)
    poke(c.io.decode.en, true)
    poke(c.io.decode.we, true)
  }

  def getFinalWrite(addr: Int, opType: BigInt, data_in: BigInt) : (Int, Boolean) = {
    val lsbs = addr & 0x00000003
    val masked_addr = addr >>> 2

    val bitMask = if (opType == mem_ops.sb) {
      0x000000ff << (8 * lsbs)
    } else if (opType == mem_ops.sh && lsbs < 3) {
      0x0000ffff << (8 * lsbs)
    } else if (opType == mem_ops.sw && lsbs == 0) {
      0xffffffff
    } else {
      // TODO: Memory should throw a trap for an ilegal memory op
      0x00000000
    }

    val final_data_in = if (opType == mem_ops.sb) {
      (data_in.toInt & 0x000000ff) << (8 * lsbs)
    } else if (opType == mem_ops.sh && lsbs < 3) {
      (data_in.toInt & 0x0000ffff) << (8 * lsbs)
    } else if (opType == mem_ops.sw && lsbs == 0) {
      data_in.toInt
    } else {
      // TODO: Memory should throw a trap for an ilegal memory op
      0x00000000
    }

    val result = if (bitMask != 0 && lsbs < 4) {
      mem_img(masked_addr) = (mem_img(masked_addr) & ~bitMask) | final_data_in
      (mem_img(masked_addr), true)
    } else {
      // TODO: Memory should throw a trap for an ilegal memory access
      (0, false)
    }

    return result
  }
}

class StoreByte(c: Memory, config: AdeptConfig) extends BaseStore(c, config) {
  private def SB(addr: Int, mem_img: HashMap[Int, Int], data_in: BigInt) = {
    setWriteSignals(mem_ops.sb.litValue(), addr, data_in)

    // Ignore output while stall is active and advance simulation
    do {
      step(1)
    } while (peek(c.io.stall) == 1)

    val finalWrite = getFinalWrite(addr, mem_ops.sb.litValue(), data_in)

    if (finalWrite._2) {
      // Read data from Adept memory, always read the entire word to ensure the
      // write mask is working properly
      setLoadSignals(mem_ops.lw, addr)

      // Ignore output while stall is active and advance simulation
      do {
        step(1)
      } while (peek(c.io.stall) == 1)

      expect(c.io.data_out, finalWrite._1)
    }
  }

  for (i <- 0 until 100) {
    val addr = rnd.nextInt(5000)
    val data_in = rnd.nextInt(128)
    SB(addr, mem_img, data_in)
  }
}

class StoreHalf(c: Memory, config: AdeptConfig) extends BaseStore(c, config) {
  private def SH(addr: Int, mem_img: HashMap[Int, Int], data_in: BigInt) = {
    setWriteSignals(mem_ops.sh.litValue(), addr, data_in)

    // Ignore output while stall is active and advance simulation
    do {
      step(1)
    } while (peek(c.io.stall) == 1)

    val finalWrite = getFinalWrite(addr, mem_ops.sh.litValue(), data_in)

    if (finalWrite._2) {
      // Read data from Adept memory, always read the entire word to ensure the
      // write mask is working properly
      setLoadSignals(mem_ops.lw.litValue(), addr)

      // Ignore output while stall is active and advance simulation
      do {
        step(1)
      } while (peek(c.io.stall) == 1)

      expect(c.io.data_out, finalWrite._1)
    }
  }

  for (i <- 0 until 100) {
    val addr = rnd.nextInt(5000)
    val data_in = rnd.nextInt(65536)
    // TODO: expect a trap when LSBs == 3
    if ((addr & 0x3) < 3) {
      SH(addr, mem_img, data_in)
    }
  }
}

class StoreWord(c: Memory, config: AdeptConfig) extends BaseStore(c, config) {
  private def SW(addr: Int, mem_img: HashMap[Int, Int], data_in: BigInt) = {
    setWriteSignals(mem_ops.sw.litValue(), addr, data_in)

    // Ignore output while stall is active and advance simulation
    do {
      step(1)
    } while (peek(c.io.stall) == 1)

    val finalWrite = getFinalWrite(addr, mem_ops.sw.litValue(), data_in)

    if (finalWrite._2) {
      // Read data from Adept memory, always read the entire word to ensure the
      // write mask is working properly
      setLoadSignals(mem_ops.lw.litValue(), addr)

      // Ignore output while stall is active and advance simulation
      do {
        step(1)
      } while (peek(c.io.stall) == 1)

      expect(c.io.data_out, finalWrite._1)
    }
  }

  for (i <- 0 until 100) {
    val addr = rnd.nextInt(5000)
    val data_in = BigInt(32, rnd)
    // TODO: expect a trap when LSBs != 0
    if ((addr & 0x3) == 0) {
      SW(addr, mem_img, data_in)
    }
  }
}
