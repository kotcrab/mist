@file:Suppress("NOTHING_TO_INLINE")

package mist.module

import kio.util.toWHex
import mist.io.BinLoader
import java.nio.charset.Charset

class ModuleMemory(
  var currentBufferAlloc: Int = INITIAL_BUFFER_ALLOC
) {
  companion object {
    private const val CHUNK_COUNT = 0xFF
    private const val CHUNK_ID_SHIFT = 24
    private const val CHUNK_MASK = 0xFFFFFF

    const val INITIAL_BUFFER_ALLOC = 0x89000000.toInt()

    fun fromData(initialMemory: List<Pair<Int, ByteArray>>): ModuleMemory {
      val memory = ModuleMemory()
      initialMemory.forEach { (start, data) ->
        data.forEachIndexed { index, byte ->
          memory.writeByte(start + index, byte.toInt())
        }
      }
      return memory
    }
  }

  val loader: BinLoader = ModuleMemoryBinLoader(this)
  private val chunks = arrayOfNulls<MemoryChunk>(CHUNK_COUNT)

  fun allocate(size: Int): Int {
    val buffer = currentBufferAlloc
    currentBufferAlloc += size
    val padding = 0x10
    currentBufferAlloc = (currentBufferAlloc / padding + 1) * padding
    return buffer
  }

  fun readWord(at: Int): Int {
    return getChunk(at).readWord(at)
  }

  fun readHalf(at: Int): Int {
    return getChunk(at).readHalf(at)
  }

  fun readByte(at: Int): Int {
    return getChunk(at).readByte(at)
  }

  fun writeWord(at: Int, value: Int) {
    getChunk(at).writeWord(at, value)
  }

  fun writeHalf(at: Int, value: Int) {
    getChunk(at).writeHalf(at, value)
  }

  fun writeByte(at: Int, value: Int) {
    getChunk(at).writeByte(at, value)
  }

  private inline fun getChunk(at: Int): MemoryChunk {
    val chunkId = at ushr CHUNK_ID_SHIFT
    return chunks[chunkId] ?: MemoryChunk().also { chunks[chunkId] = it }
  }

  private class MemoryChunk {
    val data = ByteArray(CHUNK_MASK * 4)

    inline fun readWord(at: Int): Int {
      require(at and 0b11 == 0) { "Unaligned word read at ${at.toWHex()}" }
      return (readByte(at + 3) shl 24) or
        (readByte(at + 2) shl 16) or
        (readByte(at + 1) shl 8) or
        readByte(at)
    }

    inline fun readHalf(at: Int): Int {
      require(at and 0b1 == 0) { "Unaligned half-word read at ${at.toWHex()}" }
      return (readByte(at + 1) shl 8) or readByte(at)
    }

    inline fun readByte(at: Int): Int {
      return data[at and CHUNK_MASK].toInt() and 0xFF
    }

    inline fun writeWord(at: Int, value: Int) {
      require(at and 0b11 == 0) { "Unaligned word write at ${at.toWHex()}" }
      writeByte(at + 3, value ushr 24)
      writeByte(at + 2, value ushr 16)
      writeByte(at + 1, value ushr 8)
      writeByte(at, value)
    }

    inline fun writeHalf(at: Int, value: Int) {
      require(at and 0b1 == 0) { "Unaligned half-word write at ${at.toWHex()}" }
      writeByte(at + 1, value ushr 8)
      writeByte(at, value)
    }

    inline fun writeByte(at: Int, value: Int) {
      data[at and CHUNK_MASK] = value.toByte()
    }
  }

  private class ModuleMemoryBinLoader(private val moduleMemory: ModuleMemory) : BinLoader {
    override fun readInt(at: Int): Int = moduleMemory.readWord(at)

    override fun readString(at: Int, charset: Charset): String = error("Not implemented for module memory")
  }
}
