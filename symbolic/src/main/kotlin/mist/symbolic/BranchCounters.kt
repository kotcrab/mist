package mist.symbolic

import mist.util.Counter

class BranchCounters(
  private val branches: MutableMap<Int, MutableMap<Int, Counter>> = mutableMapOf(),
) {
  fun getCounter(fromAddress: Int, toAddress: Int): Counter {
    return branches.getOrPut(fromAddress) { mutableMapOf() }
      .getOrPut(toAddress) { Counter(0) }
  }

  fun contains(fromAddress: Int): Boolean {
    return branches.contains(fromAddress)
  }

  fun copyOf(): BranchCounters {
    return BranchCounters(
      branches
        .mapValues { toAddresses ->
          toAddresses.value.mapValues { it.value.copy() }
            .toMutableMap()
        }
        .toMutableMap()
    )
  }
}
