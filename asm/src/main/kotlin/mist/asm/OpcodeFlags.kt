package mist.asm

abstract class OpcodeFlag

object MemoryRead : OpcodeFlag()
object MemoryWrite : OpcodeFlag()
object DelaySlot : OpcodeFlag()
object Jump : OpcodeFlag()
object Branch : OpcodeFlag()
object Fpu : OpcodeFlag()
object Trap : OpcodeFlag()
