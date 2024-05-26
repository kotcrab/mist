package mist.simulator

class Trace {
  val elements = mutableListOf<TraceElement>()
  val executedAddresses = mutableSetOf<Int>()

  fun isEmpty(): Boolean {
    return elements.isEmpty() && executedAddresses.isEmpty()
  }
}
