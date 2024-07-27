package mist.symbolic

import mist.module.Module

class TraceComparator(private val traceWriter: TraceWriter) {
  fun compareTraces(
    functionArgsIgnoredForCompare: Map<String, List<Int>>,
    expectedModule: Module,
    actualModule: Module,
    expectedTrace: Trace,
    actualTrace: Trace
  ): List<Message> {
    val messages = mutableListOf<Message>()
    val expectedElements = ArrayDeque(expectedTrace.elements)
    val actualElements = ArrayDeque(actualTrace.elements)

    val currentExpectedElements = mutableListOf<TraceElement>()
    val currentActualElements = mutableListOf<TraceElement>()
    while (true) {
      if (expectedElements.isEmpty() && actualElements.isEmpty()) {
        return messages
      }
      if (expectedElements.isEmpty()) {
        messages.add(Message(null, null, "Not enough trace elements in the expected trace"))
        return messages
      }
      if (actualElements.isEmpty()) {
        messages.add(Message(null, null, "Not enough trace elements in the actual trace"))
        return messages
      }
      if (!compareSyncPoints(
          messages,
          functionArgsIgnoredForCompare,
          expectedModule,
          actualModule,
          expectedElements.removeFirst(),
          actualElements.removeFirst()
        )
      ) {
        messages.add(Message(null, null, "Traces can't be fully compared"))
        return messages
      }
      currentExpectedElements.clear()
      currentActualElements.clear()
      collectElementsUntilSyncPoint(expectedElements, currentExpectedElements)
      collectElementsUntilSyncPoint(actualElements, currentActualElements)
      compareElements(messages, expectedModule, actualModule, currentExpectedElements, currentActualElements)
    }
  }

  private fun compareSyncPoints(
    messages: MutableList<Message>,
    functionArgsIgnoredForCompare: Map<String, List<Int>>,
    expectedModule: Module,
    actualModule: Module,
    expectedElement: TraceElement,
    actualElement: TraceElement
  ): Boolean {
    if (expectedElement !is TraceSyncPoint) {
      error("Expected element is not a valid sync point: ${expectedElement.toString(expectedModule)}")
    }
    if (actualElement !is TraceSyncPoint) {
      error("Actual element is not a valid sync point: $expectedElement")
    }
    val syncPointMismatchMessage = Message(
      expectedElement.pc,
      actualElement.pc,
      "Sync point mismatch: ${expectedElement.toString(expectedModule)} != ${actualElement.toString(actualModule)}"
    )
    if (expectedElement::class.java != actualElement::class.java) {
      messages.add(syncPointMismatchMessage)
      return false
    }
    when (expectedElement) {
      is TraceElement.FunctionCall -> {
        actualElement as TraceElement.FunctionCall
        if (expectedElement.name != actualElement.name || !compareExprs(
            expectedElement.arguments,
            actualElement.arguments,
            functionArgsIgnoredForCompare[expectedElement.name] ?: emptyList()
          )
        ) {
          messages.add(syncPointMismatchMessage)
        }
      }
      is TraceElement.FunctionReturn -> {
        actualElement as TraceElement.FunctionReturn
        if ((expectedElement.returnSize != actualElement.returnSize) ||
          (expectedElement.returnSize == 1 && !compareExpr(expectedElement.v0, actualElement.v0)) ||
          (expectedElement.returnSize == 2 && !compareExpr(expectedElement.v1, actualElement.v1))
        ) {
          messages.add(syncPointMismatchMessage)
        }
      }
      is TraceElement.Sync -> {
        actualElement as TraceElement.Sync
        if (expectedElement.value != actualElement.value) {
          messages.add(syncPointMismatchMessage)
        }
      }
      is TraceElement.Break -> {
        actualElement as TraceElement.Break
        if (expectedElement.value != actualElement.value) {
          messages.add(syncPointMismatchMessage)
        }
      }
      is TraceElement.DidNotTerminateWithinLimit, is TraceElement.ExecutionStart, is TraceElement.JumpOutOfFunctionBody -> {
        // Nothing to compare here
      }
      else -> error("Unexpected sync element type during compare")
    }
    return true
  }

  private fun compareElements(
    messages: MutableList<Message>,
    expectedModule: Module,
    actualModule: Module,
    expectedElements: List<TraceElement>,
    actualElements: List<TraceElement>
  ) {
    val expectedSummary = summarizeElements(expectedElements)
    val actualSummary = summarizeElements(actualElements)

    expectedSummary.reads.forEach { read ->
      if (read.address is Expr.Const && read.address.value in Engine.assumedSpRange) {
        return@forEach
      }
      val actualRead = actualSummary.reads.find { compareExpr(it.address, read.address) }
      if (actualRead == null) {
        messages.add(Message(read.pc, null, "No matching read for: ${read.toString(expectedModule)}"))
        return@forEach
      }
      val compareMessage = "${read.toString(expectedModule)} != ${actualRead.toString(actualModule)}"
      if (read.unsigned != actualRead.unsigned) {
        messages.add(Message(read.pc, actualRead.pc, "Unsigned/signed read mismatch: $compareMessage"))
      }
      if (read.unaligned != actualRead.unaligned) {
        messages.add(Message(read.pc, actualRead.pc, "Read unaligned mismatch: $compareMessage"))
      }
      if (read.shift != null && actualRead.shift != null && !compareExpr(read.shift, actualRead.shift)) {
        messages.add(Message(read.pc, actualRead.pc, "Read shift mismatch: $compareMessage"))
      }
      if (read.size != actualRead.size) {
        messages.add(Message(read.pc, actualRead.pc, "Read size mismatch: $compareMessage"))
      }
      if (!compareExpr(read.value, actualRead.value)) {
        messages.add(Message(read.pc, actualRead.pc, "Read value mismatch: $compareMessage"))
      }
    }
    val remainingActualWrites = actualSummary.writes.toMutableList()
    expectedSummary.writes.forEach { write ->
      if (write.address is Expr.Const && write.address.value in Engine.assumedSpRange) {
        return@forEach
      }
      val actualWriteIndex = remainingActualWrites.indexOfFirst { compareExpr(it.address, write.address) }
      if (actualWriteIndex == -1) {
        messages.add(Message(write.pc, null, "No matching write for: ${write.toString(expectedModule)}"))
        return@forEach
      }
      val actualWrite = remainingActualWrites.removeAt(actualWriteIndex)
      val compareMessage = "${write.toString(expectedModule)} != ${actualWrite.toString(actualModule)}"
      if (write.unaligned != actualWrite.unaligned) {
        messages.add(Message(write.pc, actualWrite.pc, "Written unaligned mismatch: $compareMessage"))
      }
      if (write.shift != null && actualWrite.shift != null && !compareExpr(write.shift, actualWrite.shift)) {
        messages.add(Message(write.pc, actualWrite.pc, "Write shift mismatch: $compareMessage"))
      }
      if (write.size != actualWrite.size) {
        messages.add(Message(write.pc, actualWrite.pc, "Written size mismatch: $compareMessage"))
      }
      if (!compareExpr(write.value, actualWrite.value)) {
        messages.add(Message(write.pc, actualWrite.pc, "Written value mismatch: $compareMessage"))
      }
    }
    remainingActualWrites.forEach { write ->
      if (write.address is Expr.Const && write.address.value in Engine.assumedSpRange) {
        return@forEach
      }
      messages.add(Message(null, write.pc, "Unexpected write: ${write.toString(actualModule)}"))
    }

    // Simplified, could be improved
    if ((expectedSummary.usedK1 == null) != (actualSummary.usedK1 == null)) {
      messages.add(Message(null, null, "K1 use mismatch"))
    }
    if ((expectedSummary.modifiedK1 == null) != (actualSummary.modifiedK1 == null)) {
      messages.add(Message(null, null, "K1 modify mismatch"))
    }
  }

  private fun summarizeElements(elements: List<TraceElement>): ElementsSummary {
    val reads = mutableListOf<TraceElement.MemoryRead>()
    val writes = mutableListOf<TraceElement.MemoryWrite>()
    var modifiedK1: Expr? = null
    var usedK1: Expr? = null
    elements.forEach {
      when (it) {
        is TraceElement.MemoryRead -> reads.add(it)
        is TraceElement.MemoryWrite -> writes.add(it)
        is TraceElement.ModifyK1 -> modifiedK1 = it.value
        is TraceElement.UseK1 -> usedK1 = it.value
        else -> error("Unexpected element type")
      }
    }
    return ElementsSummary(reads, writes, modifiedK1, usedK1)
  }

  private fun collectElementsUntilSyncPoint(elements: ArrayDeque<TraceElement>, currentElements: MutableList<TraceElement>) {
    while (elements.isNotEmpty()) {
      if (elements.first() is TraceSyncPoint) {
        return
      }
      currentElements.add(elements.removeFirst())
    }
  }

  private fun compareExprs(expectedExprs: List<Expr>, actualExprs: List<Expr>, ignoredIndexes: List<Int>): Boolean {
    expectedExprs.zip(actualExprs).forEachIndexed { index, exprs ->
      if (index in ignoredIndexes) {
        return@forEachIndexed
      }
      val (expectedExpr, actualExpr) = exprs
      if (!compareExpr(expectedExpr, actualExpr)) {
        return false
      }
    }
    return true
  }

  private fun compareExpr(expectedExpr: Expr, actualExpr: Expr): Boolean {
    return expectedExpr is Expr.Const && actualExpr is Expr.Const && expectedExpr.value == actualExpr.value
  }

  private fun TraceElement.toString(module: Module): String {
    return "[${traceWriter.writeElementToString(module, this)}]"
  }

  data class Message(val relatedExpectedPc: Int?, val relatedActualPc: Int?, val message: String)

  private class ElementsSummary(
    val reads: List<TraceElement.MemoryRead>,
    val writes: List<TraceElement.MemoryWrite>,
    val modifiedK1: Expr?,
    val usedK1: Expr?,
  )
}
