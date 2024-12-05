package mist.asm

import mist.io.BinLoader

interface Disassembler<Instruction : Instr> {
  fun disassemble(loader: BinLoader, funcDef: FunctionDef): Disassembly<Instruction>

  fun disassembleInstruction(loader: BinLoader, at: Int): Instruction
}
