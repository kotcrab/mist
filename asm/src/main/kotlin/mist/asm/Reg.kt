package mist.asm

abstract class Reg(val name: String, val id: Int, val bitsSize: Int) {
  protected companion object {
    fun <T : Reg> forId(regs: Array<T>, id: Int): T {
      if (id == -1) throw DisassemblerException("Can't return directly inaccessible register")
      regs.forEach {
        if (id == it.id) return it
      }
      throw DisassemblerException("No such register id: $id")
    }
  }

  override fun toString(): String {
    return name
  }
}
