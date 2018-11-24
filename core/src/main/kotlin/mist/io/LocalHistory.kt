/*
 * mist - interactive disassembler and decompiler
 * Copyright (C) 2018 Pawel Pastuszak
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package mist.io

import kio.LERandomAccessFile
import kio.util.child
import java.io.File
import java.time.Instant
import java.util.zip.Deflater
import java.util.zip.Inflater

/** @author Kotcrab */

class LocalHistory(storeDir: File) {
    private val index = storeDir.child("localHistory.index")
    private val data = storeDir.child("localHistory.data")
    private val indexRaf: LERandomAccessFile
    private val dataRaf: LERandomAccessFile

    private var compressedOut = ByteArray(1024)
    private var indexLocked = false

    init {
        if (index.exists() == false) index.createNewFile()
        if (data.exists() == false) data.createNewFile()
        indexRaf = LERandomAccessFile(index)
        dataRaf = LERandomAccessFile(data)
    }

    fun commit(data: ByteArray) {
        if (indexLocked) error("index is locked")
        if (data.size > compressedOut.size) {
            compressedOut = ByteArray(data.size)
        }
        val compressor = Deflater()
        compressor.setInput(data)
        compressor.finish()
        val compressedSize = compressor.deflate(compressedOut)
        compressor.end()
        val offset = writeEntryData(compressedSize)
        writeEntry(LocalHistoryEntry(Instant.now().epochSecond, offset, compressedSize, data.size))
    }

    fun read(entry: LocalHistoryEntry): ByteArray {
        val compressed = ByteArray(entry.compressedSize)
        val decompressed = ByteArray(entry.decompressedSize)
        dataRaf.seek(entry.offset)
        dataRaf.readFully(compressed)
        val decompressor = Inflater()
        decompressor.setInput(compressed)
        decompressor.inflate(decompressed)
        decompressor.end()
        return decompressed
    }

    fun entryReader() = EntryReader()

    private fun writeEntryData(compressedSize: Int): Long {
        val offset = dataRaf.length()
        dataRaf.seek(offset)
        dataRaf.write(compressedOut, 0, compressedSize)
        return offset
    }

    private fun writeEntry(entry: LocalHistoryEntry) {
        indexRaf.seek(indexRaf.length())
        indexRaf.writeLong(entry.epochSecond)
        indexRaf.writeLong(entry.offset)
        indexRaf.writeInt(entry.compressedSize)
        indexRaf.writeInt(entry.decompressedSize)
    }

    fun dispose() {
        indexRaf.close()
        dataRaf.close()
    }

    inner class EntryReader : Iterator<LocalHistoryEntry> {
        private val length = indexRaf.length()
        private var currentPos = 0L

        init {
            indexRaf.seek(0)
            indexLocked = true
        }

        override fun hasNext(): Boolean {
            if (currentPos == length) {
                indexLocked = false
                return false
            }
            return true
        }

        override fun next(): LocalHistoryEntry {
            val epochSecond = indexRaf.readLong()
            val offset = indexRaf.readLong()
            val compressedSize = indexRaf.readInt()
            val decompressedSize = indexRaf.readInt()
            currentPos = indexRaf.filePointer
            return LocalHistoryEntry(epochSecond, offset, compressedSize, decompressedSize)
        }
    }
}

class LocalHistoryEntry(val epochSecond: Long, val offset: Long, val compressedSize: Int, val decompressedSize: Int)
