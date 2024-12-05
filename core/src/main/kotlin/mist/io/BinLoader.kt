package mist.io

import java.nio.charset.Charset

interface BinLoader {
  fun readInt(at: Int): Int

  fun readString(at: Int, charset: Charset = Charsets.US_ASCII): String
}
