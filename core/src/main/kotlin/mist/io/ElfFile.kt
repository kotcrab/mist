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
import kio.util.toUnsignedInt
import kio.util.toWHex
import java.io.File

/** @author Kotcrab */
class ElfFile(val bytes: ByteArray) {
    constructor(file: File) : this(file.readBytes())

    lateinit var header: ElfHeader
        private set
    val programHeaders: Array<ElfProgramHeader>
    val sectionHeaders: Array<ElfSectionHeader>

    init {
        val programHeaders = mutableListOf<ElfProgramHeader>()
        val sectionHeaders = mutableListOf<ElfSectionHeader>()

        with(KioInputStream(bytes)) {

            if (readByte().toUnsignedInt() != 0x7F || readString(3) != "ELF") error("Not an ELF file")
            header = ElfHeader(
                    entryPoint = readInt(at = 0x18),
                    programHeaderOffset = readInt(at = 0x1C),
                    programHeaderSize = readShort(at = 0x2A).toInt(),
                    programHeaderCount = readShort(at = 0x2C).toInt(),
                    sectionHeaderOffset = readInt(at = 0x20),
                    sectionHeaderSize = readShort(at = 0x2E).toInt(),
                    sectionHeaderCount = readShort(at = 0x30).toInt())

            setPos(header.programHeaderOffset)
            repeat(header.programHeaderCount) {
                val progBytes = readBytes(header.programHeaderSize)
                with(KioInputStream(progBytes)) {
                    programHeaders.add(ElfProgramHeader(
                            offset = readInt(at = 0x04),
                            vAddr = readInt(at = 0x08),
                            memSize = readInt(at = 0x14)))
                }
            }

            setPos(header.sectionHeaderOffset)
            repeat(header.sectionHeaderCount) {
                val sectBytes = readBytes(header.sectionHeaderSize)
                with(KioInputStream(sectBytes)) {
                    sectionHeaders.add(ElfSectionHeader(
                            type = readInt(at = 0x04),
                            vAddr = readInt(at = 0x0C),
                            offset = readInt(at = 0x10),
                            size = readInt(at = 0x14)))
                }
            }
        }

        this.programHeaders = programHeaders.toTypedArray()
        this.sectionHeaders = sectionHeaders.toTypedArray()
    }
}

class ElfHeader(val entryPoint: Int,
                val programHeaderOffset: Int, val programHeaderSize: Int, val programHeaderCount: Int,
                val sectionHeaderOffset: Int, val sectionHeaderSize: Int, val sectionHeaderCount: Int) {
    override fun toString(): String {
        return "ElfHeader(entryPoint=${entryPoint.toWHex()}, programHeaderOffset=${programHeaderOffset.toWHex()}, programHeaderSize=${programHeaderSize.toWHex()}, " +
                "programHeaderCount=${programHeaderCount.toWHex()}, sectionHeaderOffset=${sectionHeaderOffset.toWHex()}, sectionHeaderSize=${sectionHeaderSize.toWHex()}, " +
                "sectionHeaderCount=${sectionHeaderCount.toWHex()})"
    }
}

class ElfProgramHeader(val offset: Int, val vAddr: Int, val memSize: Int) {
    override fun toString(): String {
        return "ElfProgramHeader(offset=${offset.toWHex()}, vAddr=${vAddr.toWHex()}, memSize=${memSize.toWHex()})"
    }
}

class ElfSectionHeader(val type: Int, val vAddr: Int, val offset: Int, val size: Int) {
    override fun toString(): String {
        return "ElfSectionHeader(type=${type.toWHex()}, vAddr=${vAddr.toWHex()}, offset=${offset.toWHex()}, size=${size.toWHex()})"
    }
}
