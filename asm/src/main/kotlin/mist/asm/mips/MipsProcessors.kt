package mist.asm.mips

import mist.asm.Processor
import mist.asm.mips.allegrex.AllegrexProcessor

fun mipsCommon(
  legacyOrigin: LegacyMipsProcessor? = MipsIProcessor,
  modernOrigin: ModernMipsProcessor? = Mips32r1Processor,
  allegrex: Boolean = true
): Array<MipsProcessor> {
  return if (allegrex) {
    arrayOf(*mipsLegacy(legacyOrigin), *mipsModern(modernOrigin), AllegrexProcessor)
  } else {
    arrayOf(*mipsLegacy(legacyOrigin), *mipsModern(modernOrigin))
  }
}

fun mipsLegacy(origin: LegacyMipsProcessor? = MipsIProcessor): Array<MipsProcessor> {
  return when (origin) {
    MipsIProcessor -> arrayOf(MipsIProcessor, MipsIIProcessor, MipsIIIProcessor, MipsIVProcessor)
    MipsIIProcessor -> arrayOf(MipsIIProcessor, MipsIIIProcessor, MipsIVProcessor)
    MipsIIIProcessor -> arrayOf(MipsIIIProcessor, MipsIVProcessor)
    MipsIVProcessor -> arrayOf(MipsIVProcessor)
    null -> emptyArray()
  }
}

fun mipsModern(origin: ModernMipsProcessor? = Mips32r1Processor): Array<MipsProcessor> {
  return when (origin) {
    Mips32r1Processor -> arrayOf(Mips32r1Processor, Mips32r2Processor, Mips32r3Processor, Mips32r5Processor)
    Mips32r2Processor -> arrayOf(Mips32r2Processor, Mips32r3Processor, Mips32r5Processor)
    Mips32r3Processor -> arrayOf(Mips32r3Processor, Mips32r5Processor)
    Mips32r5Processor -> arrayOf(Mips32r3Processor)
    null -> emptyArray()
  }
}

abstract class MipsProcessor(name: String) : Processor(name)

sealed class LegacyMipsProcessor(name: String) : MipsProcessor(name)
object MipsIProcessor : LegacyMipsProcessor("MIPS I")
object MipsIIProcessor : LegacyMipsProcessor("MIPS II")
object MipsIIIProcessor : LegacyMipsProcessor("MIPS III")
object MipsIVProcessor : LegacyMipsProcessor("MIPS IV")

sealed class ModernMipsProcessor(name: String) : MipsProcessor(name)
object Mips32r1Processor : ModernMipsProcessor("MIPS32r1")
object Mips32r2Processor : ModernMipsProcessor("MIPS32r2")
object Mips32r3Processor : ModernMipsProcessor("MIPS32r3")
object Mips32r5Processor : ModernMipsProcessor("MIPS32r5")
