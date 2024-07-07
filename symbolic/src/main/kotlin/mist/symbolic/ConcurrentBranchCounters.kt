package mist.symbolic

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class ConcurrentBranchCounters(
  private val branches: ConcurrentHashMap<Int, ConcurrentHashMap<Int, AtomicInteger>> = ConcurrentHashMap()
) {
  fun getCounter(fromAddress: Int, toAddress: Int): AtomicInteger {
    return branches.getOrPut(fromAddress) { ConcurrentHashMap() }
      .getOrPut(toAddress) { AtomicInteger(0) }
  }

  fun contains(fromAddress: Int): Boolean {
    return branches.contains(fromAddress)
  }
}
