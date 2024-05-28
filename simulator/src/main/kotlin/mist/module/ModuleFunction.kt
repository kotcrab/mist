package mist.module

data class ModuleFunction(
  val name: String,
  val entryPoint: Int,
  val maxAddress: Int,
  val length: Int,
  val type: Type,
) {
  enum class Type {
    EXPORT,
    IMPORT,
    IMPLEMENTATION,
  }

  fun calculateCoverage(executedAddresses: Set<Int>): Coverage {
    var executed = 0
    var total = 0
    for (address in entryPoint..maxAddress step 4) {
      if (address in executedAddresses) {
        executed++
      }
      total++
    }
    return Coverage(executed, total)
  }
}

