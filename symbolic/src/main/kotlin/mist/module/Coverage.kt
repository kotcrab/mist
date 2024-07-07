package mist.module

import java.util.*

data class Coverage(
  val executed: Int,
  val total: Int,
) {
  val percent = executed * 100.0 / total

  override fun toString(): String {
    return "$executed/$total ${"%.02f".format(Locale.US, percent)}%"
  }
}

fun Map<String, Coverage>.toCoverageSummary(): String {
  return entries
    .sortedBy { it.value.percent }
    .joinToString(separator = "\n", postfix = "\n\nTotal: ${values.sumCoverages()}\n") {
      "${"%-16s".format(it.value)}: ${it.key}"
    }
}

fun Iterable<Coverage>.sumCoverages(): Coverage {
  return this.reduce { acc, coverage ->
    Coverage(
      executed = acc.executed + coverage.executed,
      total = acc.total + coverage.total
    )
  }
}
