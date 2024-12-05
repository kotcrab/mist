@file:Suppress("unused")

package mist.io

import kio.KioInputStream
import kio.util.toHex
import kio.util.toUnsignedInt
import java.io.File

class ElfFile(val bytes: ByteArray) {
  constructor(file: File) : this(file.readBytes())

  val ident: ElfIdent
  val header: ElfHeader
  val progHeaders: Array<ElfProgHeader>
  val sectHeaders: Array<ElfSectHeader>

  init {
    val progHeaders = mutableListOf<ElfProgHeader>()
    val sectHeaders = mutableListOf<ElfSectHeader>()
    with(KioInputStream(bytes)) {
      if (readByte().toUnsignedInt() != 0x7F || readString(3) != "ELF") error("not an ELF file")
      ident = ElfIdent(
        clazz = readByte(at = 0x04).toUnsignedInt(),
        data = readByte(at = 0x05).toUnsignedInt(),
        version = readByte(at = 0x06).toUnsignedInt(),
        abi = readByte(at = 0x07).toUnsignedInt(),
        abiVersion = readByte(at = 0x08).toUnsignedInt()
      )
      header = ElfHeader(
        type = readShort(at = 0x10).toUnsignedInt(),
        machine = readShort(at = 0x12).toUnsignedInt(),
        version = readByte(at = 0x14).toUnsignedInt(),
        entry = readInt(at = 0x18),
        progHeaderOffset = readInt(at = 0x1C),
        sectHeaderOffset = readInt(at = 0x20),
        flags = readInt(at = 0x24),
        headerSize = readShort(at = 0x28).toUnsignedInt(),
        progHeaderSize = readShort(at = 0x2A).toUnsignedInt(),
        progHeaderCount = readShort(at = 0x2C).toUnsignedInt(),
        sectHeaderSize = readShort(at = 0x2E).toUnsignedInt(),
        sectHeaderCount = readShort(at = 0x30).toUnsignedInt(),
        sectHeaderStrIdx = readShort(at = 0x32).toUnsignedInt()
      )
      setPos(header.progHeaderOffset)
      repeat(header.progHeaderCount) {
        val progBytes = readBytes(header.progHeaderSize)
        with(KioInputStream(progBytes)) {
          progHeaders.add(
            ElfProgHeader(
              type = readInt(at = 0x00),
              offset = readInt(at = 0x04),
              vAddr = readInt(at = 0x08),
              pAddr = readInt(at = 0x0C),
              fileSize = readInt(at = 0x10),
              memSize = readInt(at = 0x14),
              flags = readInt(at = 0x18),
              align = readInt(at = 0x1C)
            )
          )
        }
      }
      setPos(header.sectHeaderOffset)
      repeat(header.sectHeaderCount) {
        val sectBytes = readBytes(header.sectHeaderSize)
        with(KioInputStream(sectBytes)) {
          sectHeaders.add(
            ElfSectHeader(
              name = readInt(at = 0x00),
              type = readInt(at = 0x04),
              flags = readInt(at = 0x08),
              vAddr = readInt(at = 0x0C),
              offset = readInt(at = 0x10),
              size = readInt(at = 0x14),
              link = readInt(at = 0x18),
              info = readInt(at = 0x1C),
              align = readInt(at = 0x20),
              entSize = readInt(at = 0x24)
            )
          )
        }
      }
    }
    this.progHeaders = progHeaders.toTypedArray()
    this.sectHeaders = sectHeaders.toTypedArray()
  }
}

class ElfIdent(
  val clazz: Int,
  val data: Int,
  val version: Int,
  val abi: Int,
  val abiVersion: Int
) {
  override fun toString(): String {
    return "ElfIdent(clazz=${clazz.toHex()}, data=${data.toHex()}, version=${version.toHex()}, " +
      "abi=${abi.toHex()}, abiVersion=${abiVersion.toHex()})"
  }
}

class ElfHeader(
  val type: Int,
  val machine: Int,
  val version: Int,
  val entry: Int,
  val progHeaderOffset: Int,
  val sectHeaderOffset: Int,
  val flags: Int,
  val headerSize: Int,
  val progHeaderSize: Int,
  val progHeaderCount: Int,
  val sectHeaderSize: Int,
  val sectHeaderCount: Int,
  val sectHeaderStrIdx: Int
) {
  override fun toString(): String {
    return "ElfHeader(type=${type.toHex()}, machine=${machine.toHex()}, version=${version.toHex()}, " +
      "entryPoint=${entry.toHex()}, progHeaderOffset=${progHeaderOffset.toHex()}, " +
      "sectHeaderOffset=${sectHeaderOffset.toHex()}, flags=${flags.toHex()}, headerSize=${headerSize.toHex()}, " +
      "progHeaderSize=${progHeaderSize.toHex()}, progHeaderCount=${progHeaderCount.toHex()}, " +
      "sectHeaderSize=${sectHeaderSize.toHex()}, sectHeaderCount=${sectHeaderCount.toHex()}, " +
      "sectHeaderStrIdx=${sectHeaderStrIdx.toHex()})"
  }
}

class ElfProgHeader(
  val type: Int,
  val offset: Int,
  val vAddr: Int,
  val pAddr: Int,
  val fileSize: Int,
  val memSize: Int,
  val flags: Int,
  val align: Int
) {
  override fun toString(): String {
    return "ElfProgHeader(type=${type.toHex()}, offset=${offset.toHex()}, vAddr=${vAddr.toHex()}, pAddr=${pAddr.toHex()}, " +
      "fileSize=${fileSize.toHex()}, memSize=${memSize.toHex()}, flags=${flags.toHex()}, align=${align.toHex()})"
  }
}

class ElfSectHeader(
  val name: Int,
  val type: Int,
  val flags: Int,
  val vAddr: Int,
  val offset: Int,
  val size: Int,
  val link: Int,
  val info: Int,
  val align: Int,
  val entSize: Int
) {
  override fun toString(): String {
    return "ElfSectHeader(name=${name.toHex()}, type=${type.toHex()}, flags=${flags.toHex()}, vAddr=${vAddr.toHex()}, " +
      "offset=${offset.toHex()}, size=${size.toHex()}, link=${link.toHex()}, info=${info.toHex()}, align=${align.toHex()}, " +
      "entSize=${entSize.toHex()})"
  }
}

object ElfType {
  const val NONE = 0x0
  const val REL = 0x1
  const val EXEC = 0x2
  const val DYN = 0x3
  const val CORE = 0x4
  const val LOOS = 0xFE00
  const val HIOS = 0xFEFF
  const val LOPROC = 0xFF00
  const val HIPROC = 0xFFFF
}

object ElfMachine {
  const val Unknown = 0x00
  const val SPARC = 0x02
  const val x86 = 0x03
  const val MIPS = 0x08
  const val PowerPC = 0x14
  const val S390 = 0x16
  const val ARM = 0x28
  const val SuperH = 0x2A
  const val IA_64 = 0x32
  const val X86_64 = 0x3E
  const val AArch64 = 0xB7
  const val RISC_V = 0xF3
}

object ElfProgHeaderType {
  const val NULL = 0x0
  const val LOAD = 0x1
  const val DYNAMIC = 0x2
  const val INTERP = 0x3
  const val NOTE = 0x4
  const val SHLIB = 0x5
  const val PHDR = 0x6
  const val LOOS = 0x60000000
  const val HIOS = 0x6FFFFFFF
  const val LOPROC = 0x70000000
  const val HIPROC = 0x7FFFFFFF
}

object ElfSectHeaderType {
  const val NULL = 0x0
  const val PROGBITS = 0x1
  const val SYMTAB = 0x2
  const val STRTAB = 0x3
  const val RELA = 0x4
  const val HASH = 0x5
  const val DYNAMIC = 0x6
  const val NOTE = 0x7
  const val NOBITS = 0x8
  const val REL = 0x9
  const val SHLIB = 0xA
  const val DYNSYM = 0xB
  const val INIT_ARRAY = 0xE
  const val FINI_ARRAY = 0xF
  const val PREINIT_ARRAY = 0x10
  const val GROUP = 0x11
  const val SYMTAB_SHNDX = 0x12
  const val NUM = 0x13
  const val LOOS = 0x60000000
  const val PSP_RELOC = 0x700000A0
}

object ElfSectHeaderFlags {
  const val WRITE = 0x1
  const val ALLOC = 0x2
  const val EXECINSTR = 0x4
  const val MERGE = 0x10
  const val STRINGS = 0x20
  const val INFO_LINK = 0x40
  const val LINK_ORDER = 0x80
  const val OS_NONCONFORMING = 0x100
  const val GROUP = 0x200
  const val TLS = 0x400
  const val MASKOS = 0x0FF00000
  const val MASKPROC = 0xF0000000.toInt()
}
