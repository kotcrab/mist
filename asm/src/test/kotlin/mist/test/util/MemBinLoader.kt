package mist.test.util

import kio.KioInputStream
import mist.io.BinLoader
import java.nio.charset.Charset

class MemBinLoader(bytes: ByteArray) : BinLoader {
  private val input = KioInputStream(bytes)

  override fun readInt(at: Int): Int = input.readInt(at)
  override fun readString(at: Int, charset: Charset): String = input.run {
    setPos(at)
    readNullTerminatedString(charset)
  }
}
