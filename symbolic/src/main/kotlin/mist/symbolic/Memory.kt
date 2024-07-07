@file:Suppress("NOTHING_TO_INLINE")

package mist.symbolic

import kio.util.toWHex

class Memory private constructor(
  val captures: MutableList<List<Expr.Store>>,
  private val stores: MutableList<Expr.Store>,
  var hwWordsReadsSinceLastBranch: Int = 0,
  var writesSinceLastBranch: Int = 0,
  private var currentBufferAlloc: Int = BUFFER_ALLOC_START
) {
  companion object {
    private const val BUFFER_ALLOC_START = 0x8800000
    private val hwRanges = setOf(0xBC, 0xBD, 0xBE)
  }

  constructor() : this(mutableListOf(), mutableListOf())

  var concrete = false
  var ignoreIllegalAccess = false

  val onReadWordHooks = mutableMapOf<Int, () -> BvExpr?>()
  val onReadHalfHooks = mutableMapOf<Int, () -> BvExpr?>()
  val onReadByteHooks = mutableMapOf<Int, () -> BvExpr?>()

  fun copyOf(): Memory {
    return Memory(
      captures = captures.toMutableList(),
      stores = stores.toMutableList(),
      hwWordsReadsSinceLastBranch = hwWordsReadsSinceLastBranch,
      writesSinceLastBranch = writesSinceLastBranch,
      currentBufferAlloc = currentBufferAlloc
    )
  }

  fun resetBranchAccessTracking() {
    hwWordsReadsSinceLastBranch = 0
    writesSinceLastBranch = 0
  }

  fun allocate(size: Int, initByte: Int = 0xCD): Int {
    val buffer = currentBufferAlloc
    repeat(size) {
      writeByte(Expr.Const.of(buffer + it), Expr.Const.of(initByte))
    }
    currentBufferAlloc += size
    val pad = 0x10
    currentBufferAlloc = (currentBufferAlloc / pad + 1) * pad
    return buffer
  }

  fun writeByte(at: BvExpr, expr: BvExpr) {
    writesSinceLastBranch++
    writeByteInline(at, Expr.Extract.of(expr, 7, 0))
  }

  fun writeHalf(at: BvExpr, expr: BvExpr) {
    writesSinceLastBranch++
    if (at is Expr.Const && !ignoreIllegalAccess) {
      require(at.value and 0b1 == 0) { "Unaligned half-word write at ${(at.value).toWHex()}" }
    }
    writeByteInline(at, Expr.Extract.of(expr, 7, 0))
    writeByteInline(Expr.Binary.of(BinaryOp.Add, at, Expr.Const.of(1)), Expr.Extract.of(expr, 15, 8))
  }

  fun writeWord(at: BvExpr, expr: BvExpr) {
    writesSinceLastBranch++
    if (at is Expr.Const && !ignoreIllegalAccess) {
      require(at.value and 0b11 == 0) { "Unaligned word write at ${(at.value).toWHex()}" }
    }
    writeByteInline(at, Expr.Extract.of(expr, 7, 0))
    writeByteInline(Expr.Binary.of(BinaryOp.Add, at, Expr.Const.of(1)), Expr.Extract.of(expr, 15, 8))
    writeByteInline(Expr.Binary.of(BinaryOp.Add, at, Expr.Const.of(2)), Expr.Extract.of(expr, 23, 16))
    writeByteInline(Expr.Binary.of(BinaryOp.Add, at, Expr.Const.of(3)), Expr.Extract.of(expr, 31, 24))
  }

  private inline fun writeByteInline(at: BvExpr, expr: BvExpr) {
    stores.add(Expr.Store.of(at, expr))
  }

  fun readByte(at: BvExpr): BvExpr {
    return Expr.Unary.of(UnaryOp.SebMem, readByteInline(at))
  }

  fun readByteUnsigned(at: BvExpr): BvExpr {
    return Expr.Unary.of(UnaryOp.ZebMem, readByteInline(at))
  }

  fun readHalf(at: BvExpr): BvExpr {
    return Expr.Unary.of(UnaryOp.SehMem, readHalfInline(at))
  }

  fun readHalfUnsigned(at: BvExpr): BvExpr {
    return Expr.Unary.of(UnaryOp.ZehMem, readHalfInline(at))
  }

  fun readWord(at: BvExpr): BvExpr {
    if (at is Expr.Const) {
      if (!ignoreIllegalAccess) {
        require(at.value and 0b11 == 0) { "Unaligned word read at ${(at.value).toWHex()}" }
      }

      if (at.value ushr 24 in hwRanges) {
        hwWordsReadsSinceLastBranch++
      }

      val hookValue = onReadWordHooks[at.value]?.invoke()
      if (hookValue != null) {
        return hookValue
      }
    }

    val b0 = readByteInline(at)
    val b1 = readByteInline(Expr.Binary.of(BinaryOp.Add, at, Expr.Const.of(1)))
    val b2 = readByteInline(Expr.Binary.of(BinaryOp.Add, at, Expr.Const.of(2)))
    val b3 = readByteInline(Expr.Binary.of(BinaryOp.Add, at, Expr.Const.of(3)))
    return if (b3 is Expr.Const && b2 is Expr.Const && b1 is Expr.Const && b0 is Expr.Const) {
      Expr.Const.of(
        (b3.value shl 24) or
          (b2.value shl 16) or
          (b1.value shl 8) or
          b0.value
      )
    } else {
      Expr.Concat.of(b3, Expr.Concat.of(b2, Expr.Concat.of(b1, b0)))
    }
  }

  private inline fun readHalfInline(at: BvExpr): BvExpr {
    if (at is Expr.Const) {
      if (!ignoreIllegalAccess) {
        require(at.value and 0b1 == 0) { "Unaligned half-word read at ${(at.value).toWHex()}" }
      }
      val hookValue = onReadHalfHooks[at.value]?.invoke()
      if (hookValue != null) {
        return hookValue
      }
    }

    val b0 = readByteInline(at)
    val b1 = readByteInline(Expr.Binary.of(BinaryOp.Add, at, Expr.Const.of(1)))
    return if (b1 is Expr.Const && b0 is Expr.Const) {
      Expr.Const.of(
        (b1.value shl 8) or
          b0.value
      )
    } else {
      Expr.Concat.of(b1, b0)
    }
  }

  private inline fun readByteInline(at: BvExpr): BvExpr {
    if (at is Expr.Const) {
      val hookValue = onReadByteHooks[at.value]?.invoke()
      if (hookValue != null) {
        return hookValue
      }

      for (i in stores.indices.reversed()) {
        val store = stores[i]
        if (store.address !is Expr.Const) {
          break
        }
        if (store.address.value == at.value) {
          return store.value
        }
      }

      if (concrete) {
        return Expr.ZERO
      }
    }
    captureStores()
    return Expr.Select.of(at, captures.size - 1)
  }

  private fun captureStores() {
    if (stores.size > 0) {
      captures.add(stores.toList())
      stores.clear()
    }
  }
}
