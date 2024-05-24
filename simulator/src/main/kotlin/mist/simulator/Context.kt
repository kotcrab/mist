@file:Suppress("NOTHING_TO_INLINE")

package mist.simulator

import kio.util.toWHex
import mist.asm.Reg
import mist.asm.mips.GprReg

class Context {
    companion object {
        private const val CHUNK_COUNT = 0xFF
        private const val CHUNK_ID_SHIFT = 24
        private const val CHUNK_MASK = 0xFFFFFF

        private const val BUF_ALLOC_START = 0x8800000
    }

    var pc = 0
    var lo = 0
    var hi = 0
    val gpr = IntArray(32)
    val memory = Memory()

    val functionHandlers = mutableMapOf<String, FunctionHandler>()

    fun registerFunctionHandler(handler: NamedFunctionHandler) {
        functionHandlers[handler.name] = handler
    }

    inline fun writeGpr(reg: Reg, value: Int) {
        require(reg is GprReg)
        if (reg.id == 0) {
            return
        }
        gpr[reg.id] = value
    }

    inline fun readGpr(reg: Reg): Int {
        return gpr[reg.id]
    }

    class Memory {
        private val chunks = arrayOfNulls<MemoryChunk>(CHUNK_COUNT)
        private var currentBufAlloc = BUF_ALLOC_START

        val onReadWordHooks = mutableMapOf<Int, () -> Int?>()
        val onReadHalfHooks = mutableMapOf<Int, () -> Int?>()
        val onReadByteHooks = mutableMapOf<Int, () -> Int?>()

        fun readWord(at: Int): Int {
            return onReadWordHooks[at]?.invoke()
                ?: getChunk(at).readWord(at)
        }

        fun readHalf(at: Int): Int {
            return onReadHalfHooks[at]?.invoke()
                ?: return getChunk(at).readHalf(at)
        }

        fun readByte(at: Int): Int {
            return onReadByteHooks[at]?.invoke()
                ?: return getChunk(at).readByte(at)
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
            val useChunkId = if (chunkId and 0xE0 == 0xA0) (chunkId and 0xE0.inv() or 0x80) else chunkId // KUNCACHED
            return chunks[useChunkId] ?: MemoryChunk().also { chunks[useChunkId] = it }
        }

        fun allocBuf(size: Int, initByte: Int = 0xCD): Int {
            val buf = currentBufAlloc
            repeat(size) {
                writeByte(buf + it, initByte)
            }
            currentBufAlloc += size
            val pad = 0x10
            currentBufAlloc = (currentBufAlloc / pad + 1) * pad
            return buf
        }
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
}
