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
import kio.util.arrayCopy
import kio.util.toHex
import kio.util.toWHex
import mist.util.DecompLog
import mist.util.logTag
import java.nio.charset.Charset

class PspElfLoader(private val elf: ElfFile, private val log: DecompLog) : BinLoader {
    private val tag = logTag()
    private val fragments = mutableMapOf<IntRange, MemoryFragment>()

    init {
        preLoadCheck()
        val elfBytes = KioInputStream(elf.bytes)
        loadProgramHeaders(elfBytes)
        if (elf.header.type != ElfType.EXEC) {
            applyRelocations(elfBytes)
        }
    }

    private fun preLoadCheck() {
        log.info(tag, "load PSP elf, entry point ${elf.header.entry.toHex()}")
        if (elf.ident.clazz != 0x1 || elf.ident.data != 0x1 || elf.ident.version != 0x1 || elf.header.version != 0x1) {
            log.warn(tag, "expected elf ident class, data and version fields to be set to 0x1, ignoring")
        }
        if (elf.ident.abi != 0x0 || elf.ident.abiVersion != 0x0) {
            log.warn(tag, "expected elf ident abi and abi version fields to be set to 0x0, ignoring")
        }
        if (elf.header.machine != ElfMachine.MIPS) {
            log.panic(tag, "expected elf header machine field to be MIPS")
        }
    }

    private fun loadProgramHeaders(elfBytes: KioInputStream) {
        elf.progHeaders.forEach { prog ->
            if (prog.type == ElfProgHeaderType.LOAD) {
                log.trace(
                    tag, "load program header at ${prog.vAddr.toHex()}, memSize: ${prog.memSize.toHex()}, " +
                            "fileSize: ${prog.fileSize.toHex()}"
                )
                val vRange = IntRange(prog.vAddr, prog.vAddr + prog.memSize)
                val vBytes = ByteArray(prog.memSize)
                elfBytes.setPos(prog.offset)
                val bytes = elfBytes.readBytes(prog.fileSize)
                arrayCopy(src = bytes, dest = vBytes)
                fragments[vRange] = MemoryFragment(vBytes)
            }
        }
    }

    private fun applyRelocations(elfBytes: KioInputStream) {
        log.trace(tag, "elf needs relocation")
        elf.sectHeaders.forEach { sect ->
            if (sect.type == ElfSectHeaderType.REL || sect.type == ElfSectHeaderType.RELA) {
                log.panic(tag, "expected relocation segment to be of PSP specific type but got SHL_REL or SHT_RELA")
            }
            if (sect.type != ElfSectHeaderType.PSP_RELLOC) return@forEach
            if (sect.entSize != 0x8) {
                log.panic(tag, "expected relocation entry size to be 0x8")
            }
            val entryCount = sect.size / sect.entSize
            log.trace(
                tag,
                "apply relocations from section at ${sect.offset.toHex()}, entry count: ${entryCount.toHex()}"
            )
            elfBytes.setPos(sect.offset)
            val relocations = mutableListOf<Relocation>()
            repeat(entryCount) {
                val addr = elfBytes.readInt()
                val relloc = elfBytes.readInt()
                val type = relloc and 0xFF
                val offsetIdx = relloc shr 8 and 0xFF
                val relocateToIdx = relloc shr 16 and 0xFF
                relocations.add(
                    Relocation(
                        addr,
                        type,
                        elf.progHeaders[offsetIdx].vAddr,
                        elf.progHeaders[relocateToIdx].vAddr
                    )
                )
            }
            relocations.forEachIndexed { index, relloc ->
                var addr = relloc.addr
                addr += relloc.offset
                var data = readInt(addr)
                val relocateTo = relloc.rellocTo
                when (relloc.type) {
                    MipsRelocationType.MIPS_16 -> {
                        data = relocate(data, 0xFFFF, relocateTo)
                    }
                    MipsRelocationType.MIPS_32 -> {
                        data += relocateTo
                    }
                    MipsRelocationType.MIPS_26 -> {
                        data = relocate(data, 0x3FFFFFF, relocateTo shr 2) // j, jal don't include 2 lowest bits
                    }
                    MipsRelocationType.MIPS_HI16 -> {
                        var newHi = (data and 0xFFFF) shl 16
                        newHi += relocateTo
                        data = (data and 0xFFFF0000.toInt()) or (newHi shr 16)
                    }
                    MipsRelocationType.MIPS_LO16 -> {
                        data = relocate(data, 0xFFFF, relocateTo)
                    }
                    else -> log.panic(tag, "unsupported relocation type: ${relloc.type.toHex()}")
                }
                writeInt(addr, data)
            }
        }
    }

    private fun relocate(data: Int, mask: Int, relocateTo: Int): Int {
        return (data and mask.inv()) or (((data and mask) + relocateTo) and mask)
    }

    private fun writeInt(at: Int, newValue: Int) {
        for ((range, mem) in fragments) {
            if (at in range) {
                mem.writeInt(at, newValue)
                return
            }
        }
        log.panic(tag, "${at.toWHex()} not mapped to any elf fragment")
    }

    override fun readInt(at: Int): Int {
        for ((range, mem) in fragments) {
            if (at in range) {
                return mem.readInt(at)
            }
        }
        log.panic(tag, "${at.toWHex()} not mapped to any elf fragment")
    }

    override fun readString(at: Int, charset: Charset): String {
        for ((range, mem) in fragments) {
            if (at in range) {
                return mem.readString(at, charset)
            }
        }
        log.panic(tag, "${at.toWHex()} not mapped to any elf fragment")
    }

    private class MemoryFragment(val bytes: ByteArray) {
        private val bytesReader = KioInputStream(bytes)

        fun writeInt(at: Int, newValue: Int) {
            if (at > bytes.size) error("write out of bounds: ${at.toWHex()}")
            bytes[at] = (newValue and 0xFF).toByte()
            bytes[at + 1] = (newValue shr 8 and 0xFF).toByte()
            bytes[at + 2] = (newValue shr 16 and 0xFF).toByte()
            bytes[at + 3] = (newValue shr 24 and 0xFF).toByte()
        }

        fun readInt(at: Int): Int {
            if (at > bytes.size) error("read out of bounds: ${at.toWHex()}")
            bytesReader.setPos(at)
            return bytesReader.readInt()
        }

        fun readString(at: Int, charset: Charset): String {
            if (at > bytes.size) error("read out of bounds: ${at.toWHex()}")
            bytesReader.setPos(at)
            return bytesReader.readNullTerminatedString(charset)
        }
    }

    data class Relocation(val addr: Int, val type: Int, val offset: Int, val rellocTo: Int)
}

private object MipsRelocationType {
    const val MIPS_16 = 1
    const val MIPS_32 = 2
    const val MIPS_26 = 4
    const val MIPS_HI16 = 5
    const val MIPS_LO16 = 6
}
