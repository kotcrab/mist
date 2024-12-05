package mist.asm

abstract class Opcode(
  val mnemonic: String,
  val description: String = "",
  val processors: Array<out Processor>,
  val flags: Array<out OpcodeFlag> = emptyArray(),
  val use: Array<out OperandRef> = emptyArray(),
  val modify: Array<out OperandRef> = emptyArray()
)
