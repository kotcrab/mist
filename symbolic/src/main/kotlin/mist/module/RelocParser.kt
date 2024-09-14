package mist.module

import kio.KioInputStream
import kio.util.toUnsignedInt

// copied from ghidra-allegrex

object RelocParser {
  fun parseRelocationsTypeB(
    reader: KioInputStream,
    loadableCount: Int,
    programHeaderAddresses: List<Long>,
    useRebootBinMapping: Boolean,
  ): List<AllegrexRelocation.TypeB> {
    val relocations = mutableListOf<AllegrexRelocation.TypeB>()

    // based on PRXTool
    reader.setPos(2)

    val part1Size = reader.readByte().toUnsignedInt()
    val part2Size = reader.readByte().toUnsignedInt()
    val block1 = reader.pos()
    val block1Size = reader.readByte().toUnsignedInt()
    val block2 = block1 + block1Size
    reader.setPos(block2)
    val block2Size = reader.readByte().toUnsignedInt()

    val start = block2 + block2Size
    val end = reader.size

    val nBits = if (loadableCount < 3) 1 else 2

    var offset = 0
    var offsetBase = 0
    var lastPart2 = block2Size
    reader.setPos(start)
    while (reader.pos() < end) {
      val cmd = reader.readShort().toUnsignedInt()

      var part1Offset = (cmd shl (16 - part1Size)) and 0xFFFF
      part1Offset = (part1Offset shr (16 - part1Size)) and 0xFFFF
      if (part1Offset > block1Size) {
        error("Invalid part1 offset: $part1Offset")
      }
      val part1 = reader.readByte(block1 + part1Offset).toUnsignedInt()

      if (part1 and 0x1 == 0) {
        offsetBase = (cmd shl (16 - part1Size - nBits)) and 0xFFFF
        offsetBase = (offsetBase shr (16 - nBits)) and 0xFFFF
        if (offsetBase >= loadableCount) {
          error("Invalid offset base: $offsetBase")
        }

        when {
          part1 and 0x06 == 0 -> offset = cmd shr (part1Size + nBits)
          part1 and 0x06 == 4 -> offset = reader.readInt()
          else -> println("Invalid offset (part1=$part1)")
        }
      } else {
        var part2Offset = (cmd shl 16 - (part1Size + nBits + part2Size)) and 0xFFFF
        part2Offset = (part2Offset shr (16 - part2Size)) and 0xFFFF
        if (part2Offset > block2Size) {
          error("Invalid part2 offset: $part2Offset")
        }
        val part2 = reader.readByte(block2 + part2Offset).toUnsignedInt()

        var addressBase = (cmd shl (16 - part1Size - nBits)) and 0xFFFF
        addressBase = (addressBase shr (16 - nBits)) and 0xFFFF
        if (addressBase >= loadableCount) {
          error("Invalid address base: $addressBase")
        }

        when (part1 and 0x06) {
          0 -> {
            var delta = cmd
            if (delta and 0x8000 != 0) {
              delta = delta or 0xFFFF.inv()
              delta = delta shr part1Size + part2Size + nBits
              delta = delta or 0xFFFF.inv()
            } else {
              delta = delta shr part1Size + part2Size + nBits
            }
            offset += delta
          }
          2 -> {
            var delta = cmd
            if (delta and 0x8000 != 0) {
              delta = delta or 0xFFFF.inv()
            }
            delta = (delta shr (part1Size + part2Size + nBits)) shl 16
            delta = delta or reader.readShort().toUnsignedInt()
            offset += delta
          }
          4 -> {
            offset = reader.readInt()
          }
          else -> {
            error("Invalid part1 offset config: $part1")
          }
        }

//        if (offset >= programHeaders[offsetBase].fileSize) {
//          error("Invalid relocation offset (out of bounds) (offset=$offset)")
//        }

        var addend: Short = 0
        when (part1 and 0x38) {
          0 -> {
            addend = 0
          }
          0x08 -> {
            if (lastPart2 xor 0x04 != 0) {
              addend = 0
            }
          }
          0x10 -> {
            addend = reader.readShort()
          }
          else -> {
            error("Invalid addend size (part1=$part1)")
          }
        }
        lastPart2 = part2

        val type = if (useRebootBinMapping) {
          when (part2) {
            0 -> AllegrexElfRelocationConstants.R_MIPS_NONE
            1 -> AllegrexElfRelocationConstants.R_MIPS_26
            2 -> AllegrexElfRelocationConstants.R_MIPS_X_J26
            3 -> AllegrexElfRelocationConstants.R_MIPS_X_JAL26
            4, 7 -> AllegrexElfRelocationConstants.R_MIPS_16
            5 -> AllegrexElfRelocationConstants.R_MIPS_32
            6 -> AllegrexElfRelocationConstants.R_MIPS_X_HI16
            else -> error("Unsupported type B relocation: $part2")
          }
        } else {
          when (part2) {
            0 -> AllegrexElfRelocationConstants.R_MIPS_NONE
            1, 5 -> AllegrexElfRelocationConstants.R_MIPS_LO16
            2 -> AllegrexElfRelocationConstants.R_MIPS_32
            3 -> AllegrexElfRelocationConstants.R_MIPS_26
            4 -> AllegrexElfRelocationConstants.R_MIPS_X_HI16
            6 -> AllegrexElfRelocationConstants.R_MIPS_X_J26
            7 -> AllegrexElfRelocationConstants.R_MIPS_X_JAL26
            else -> error("Unsupported type B relocation: $part2")
          }
        }

        relocations.add(
          AllegrexRelocation.TypeB(
            offset,
            type,
            offsetBase,
            addressBase,
            programHeaderAddresses[offsetBase], //programHeaders[offsetBase].virtualAddress, // FIXME
            programHeaderAddresses[addressBase], //programHeaders[addressBase].virtualAddress,
            when (type) {
              AllegrexElfRelocationConstants.R_MIPS_X_HI16 -> addend.toInt()
              else -> 0
            }
          )
        )
      }
    }

    return relocations
  }
}

sealed interface AllegrexRelocation {
  val offset: Int
  val type: Int

  data class TypeA private constructor(
   override val offset: Int,
   override val type: Int,
    val relativeIndex: Int,
    val relocateToIndex: Int,
    val relative: Int,
    val relocateTo: Int,
    val linkedLoValue: Int,
  ) : AllegrexRelocation

  data class TypeB(
    override val offset: Int,
    override val type: Int,
    val offsetBaseIndex: Int,
    val addressBaseIndex: Int,
    val offsetBase: Long,
    val addressBase: Long,
    val addend: Int,
    private val unused: Int = 0,
  ) : AllegrexRelocation
}

object AllegrexElfRelocationConstants {
  const val R_MIPS_NONE = 0
  const val R_MIPS_16 = 1
  const val R_MIPS_32 = 2
  const val R_MIPS_26 = 4
  const val R_MIPS_HI16 = 5
  const val R_MIPS_LO16 = 6

  // Mapping for "new" type B relocations is rather arbitrary
  const val R_MIPS_X_HI16 = 13
  const val R_MIPS_X_J26 = 14
  const val R_MIPS_X_JAL26 = 15
}
