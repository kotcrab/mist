package mist.asm

class Disassembly<Instruction : Instr>(val def: FunctionDef, val instrs: List<Instruction>)
