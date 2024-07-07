package mist.module

import kio.util.toWHex

data class ModuleAddress(
  val address: Int,
  val symbol: Symbol?,
) {
  fun toAccessString(): String {
    var resultPrefix = ""
    var resultSuffix = ""
    if (isUncached()) {
      resultPrefix = "KUNCACHED("
      resultSuffix = ")"
    }
    val result = if (symbol != null) {
      "${symbol.name}${symbol.memberPath}"
    } else {
      cachedAddress().toWHex()
    }
    return "$resultPrefix$result$resultSuffix"
  }

  fun isUncached() = address ushr 29 == 0b101

  fun cachedAddress() = address and 0x1FFFFFFF or 0x80000000.toInt()

  data class Symbol(
    val name: String,
    val localOffset: Int,
    val memberPath: String,
  )
}
