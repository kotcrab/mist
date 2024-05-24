package mist.simulator

import mist.io.BinLoader
import java.nio.charset.Charset

class ContextBinLoader(private val ctx: Context) : BinLoader {
    override fun readInt(at: Int): Int = ctx.memory.readWord(at)

    override fun readString(at: Int, charset: Charset): String = error("Not implemented")
}
