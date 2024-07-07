package mist.util

import mist.asm.Disassembler
import mist.asm.Disassembly
import mist.asm.FunctionDef
import mist.asm.Instr
import mist.io.BinLoader
import java.util.concurrent.ConcurrentHashMap

fun <T : Instr> Disassembler<T>.cached(): Disassembler<T> = CachingDisassembler(this)

private class CachingDisassembler<T : Instr>(
  private val delegate: Disassembler<T>
) : Disassembler<T> {
  private val cache = ConcurrentHashMap<Int, T>()

  override fun disassemble(loader: BinLoader, funcDef: FunctionDef): Disassembly<T> {
    return delegate.disassemble(loader, funcDef)
  }

  override fun disassembleInstruction(loader: BinLoader, at: Int): T {
    return cache.getOrPut(at) { delegate.disassembleInstruction(loader, at) }
  }
}
