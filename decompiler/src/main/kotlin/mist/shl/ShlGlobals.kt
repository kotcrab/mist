package mist.shl

import kio.util.swap

class ShlGlobals {
  private val globals = mutableListOf<ShlGlobal>()

  @Transient
  private var accessMap = mutableMapOf<Int, ShlGlobal>()

  fun addGlobal(address: Int, name: String, refTid: Int, pointer: Boolean, comment: String) {
    checkAccessMap()
    val global = ShlGlobal(address, name, refTid, pointer, comment)
    globals.add(global)
    insertGlobal(global)
  }

  fun editGlobal(
    oldGlobal: ShlGlobal,
    newAddress: Int,
    newName: String,
    newRefTid: Int,
    newPointer: Boolean,
    newComment: String
  ) {
    val prevIdx = globals.indexOf(oldGlobal)
    delete(oldGlobal)
    val newGlobal = ShlGlobal(newAddress, newName, newRefTid, newPointer, newComment)
    globals.add(prevIdx, newGlobal)
    insertGlobal(newGlobal)
  }

  fun getGlobals(): List<ShlGlobal> {
    return globals
  }

  fun getGlobalByAddress(address: Int): ShlGlobal? {
    checkAccessMap()
    return accessMap[address]
  }

  private fun checkAccessMap() {
    if (accessMap.isNotEmpty()) return
    // perform initial init of access map after deserialization
    globals.forEach { insertGlobal(it) }
  }

  private fun insertGlobal(global: ShlGlobal) {
    accessMap[global.address] = global
  }

  fun moveUp(global: ShlGlobal) {
    val newIndex = globals.indexOf(global) - 1
    if (newIndex >= 0) {
      globals.swap(newIndex, newIndex + 1)
    }
  }

  fun moveDown(global: ShlGlobal) {
    val newIndex = globals.indexOf(global) + 1
    if (newIndex < globals.size) {
      globals.swap(newIndex, newIndex - 1)
    }
  }

  fun delete(global: ShlGlobal) {
    accessMap.remove(global.address)
    globals.remove(global)
  }
}

class ShlGlobal(val address: Int, var name: String, var refTid: Int, var pointer: Boolean, var comment: String)
