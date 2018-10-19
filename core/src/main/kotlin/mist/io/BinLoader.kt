/*
 * mist - interactive mips disassembler and decompiler
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

import kio.KioInputStream
import kio.util.toWHex
import java.nio.charset.Charset

/** @author Kotcrab */

class ElfLoader(private val elf: ElfFile) : BinLoader {
    private val bytesReader = KioInputStream(elf.bytes)

    override fun readInt(at: Int): Int {
        if (at > elf.programHeaders[0].memSize) error("read out of bounds: ${at.toWHex()}")
        bytesReader.setPos(elf.programHeaders[0].offset + at)
        return bytesReader.readInt()
    }

    override fun readString(at: Int, charset: Charset): String {
        if (at > elf.programHeaders[0].memSize) error("read out of bounds: ${at.toWHex()}")
        bytesReader.setPos(elf.programHeaders[0].offset + at)
        return bytesReader.readNullTerminatedString(charset)
    }
}

class MemBinLoader(bytes: ByteArray) : BinLoader {
    private val input = KioInputStream(bytes)

    override fun readInt(at: Int): Int = input.readInt(at)
    override fun readString(at: Int, charset: Charset): String = input.run {
        setPos(at)
        readNullTerminatedString(charset)
    }
}

interface BinLoader {
    fun readInt(at: Int): Int
    fun readString(at: Int, charset: Charset = Charsets.US_ASCII): String
}
