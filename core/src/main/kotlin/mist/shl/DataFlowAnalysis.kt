/*
 * mist - interactive disassembler and decompiler
 * Copyright (C) 2018 Pawel Pastuszak
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package mist.shl

import mist.asm.mips.Node
import mist.util.DecompLog
import mist.util.logTag
import org.apache.commons.collections4.CollectionUtils

/** @author Kotcrab */

class DataFlowAnalysis(private val graph: ShlGraph, private val log: DecompLog) {
  val tag = logTag()

  fun analyze() {
    log.trace(tag, "analysis start")
    clearCtxs()
    reachRegisters()
    liveRegisters()
    forwardAllPaths(propagateKnownValues = false)
  }

  private fun liveRegisters() {
    val workList = mutableListOf<Node<ShlInstr>>()
    workList.addAll(graph.nodes)
    while (workList.isNotEmpty()) {
      val node = workList.removeAt(0)
      val nodeIdx = graph.nodes.indexOf(node)
      val instrs = node.instrs

      val output = when {
        node.outEdges.size == 0 -> LiveFlowCtx()
        else -> {
          node.outEdges.map { it.node.getInputLiveFlowCtx() }
            .mapIndexed { index, ctx -> if (index == 0) ctx.clone() else ctx }
            .reduce({ ctx, otherCtx -> ctx.join(otherCtx) })
        }
      }
      val prevInput = node.getInputLiveFlowCtx().clone()

      instrs.reversed().forEachIndexed { revInstrIdx, instr ->
        val instrIdx = instrs.lastIndex - revInstrIdx
        val nextCtx = if (instrIdx == instrs.lastIndex) output else instrs[instrIdx + 1].liveFlowCtx
        instr.nextLiveFlowCtx.apply {
          clear()
          include(nextCtx)
        }
        instr.liveFlowCtx.apply {
          clear()
          include(nextCtx)
          nullifyVars(instr.getNullifiedVars())
          processExpr(nodeIdx, instrIdx, instr.getWriteExpr(), instr.getReadExpr())
        }
      }

      if (node.getInputLiveFlowCtx().compare(prevInput) == false) {
        node.inEdges.map { it.node }
          .filterNot { workList.contains(it) }
          .forEach { workList.add(it) }
      }
    }
  }

  private fun reachRegisters() {
    val workList = mutableListOf<Node<ShlInstr>>()
    workList.addAll(graph.nodes)
    while (workList.isNotEmpty()) {
      val node = workList.removeAt(0)
      val nodeIdx = graph.nodes.indexOf(node)
      val instrs = node.instrs

      val input = when {
        node.inEdges.size == 0 -> ReachFlowCtx()
        else -> {
          node.inEdges.map { it.node.getOutputReachFlowCtx() }
            .mapIndexed { index, ctx -> if (index == 0) ctx.clone() else ctx }
            .reduce({ ctx, otherCtx -> ctx.join(otherCtx) })
        }
      }
      val prevOutput = node.getOutputReachFlowCtx().clone()

      instrs.forEachIndexed { instrIdx, instr ->
        val prevCtx = if (instrIdx == 0) input else instrs[instrIdx - 1].reachFlowCtx
        instr.prevReachFlowCtx.apply {
          clear()
          include(prevCtx)
        }
        instr.reachFlowCtx.apply {
          clear()
          include(prevCtx)
          nullifyVars(instr.getNullifiedVars())
          processExpr(nodeIdx, instrIdx, instr.getWriteExpr())
        }
      }

      if (node.getOutputReachFlowCtx().compare(prevOutput) == false) {
        node.outEdges.map { it.node }
          .filterNot { workList.contains(it) }
          .forEach { workList.add(it) }
      }
    }
  }

  private fun forwardAllPaths(propagateKnownValues: Boolean) {
    val workList = mutableListOf<Node<ShlInstr>>()
    workList.addAll(graph.nodes)
    while (workList.isNotEmpty()) {
      val node = workList.removeAt(0)
      val instrs = node.instrs

      val input = when {
        node.inEdges.size == 0 -> DataFlowCtx()
        else -> {
          node.inEdges.map { it.node.getOutputDataFlowCtx() }
            .mapIndexed { index, ctx -> if (index == 0) ctx.clone() else ctx }
            .reduce({ ctx, otherCtx -> ctx.join(otherCtx) })
        }
      }
      val prevOutput = node.getOutputDataFlowCtx().clone()

      instrs.forEachIndexed { instrIdx, instr ->
        val prevCtx = if (instrIdx == 0) input else instrs[instrIdx - 1].dataFlowCtx
        instr.prevDataFlowCtx.apply {
          clear()
          include(prevCtx)
        }
        instr.dataFlowCtx.apply {
          clear()
          include(prevCtx)
          nullifyVars(instr.getNullifiedVars())
          processExpr(instr.getWriteExpr(), instr.getReadExpr(), propagateKnownValues)
        }
      }

      if (node.getOutputDataFlowCtx().compare(prevOutput) == false) {
        node.outEdges.map { it.node }
          .filterNot { workList.contains(it) }
          .forEach { workList.add(it) }
      }
    }
  }

  private fun clearCtxs() {
    graph.nodes.forEach { node ->
      node.instrs.forEach { instr ->
        instr.dataFlowCtx.clear()
        instr.prevDataFlowCtx.clear()
        instr.reachFlowCtx.clear()
        instr.prevReachFlowCtx.clear()
        instr.liveFlowCtx.clear()
        instr.nextLiveFlowCtx.clear()
      }
    }
  }

  private fun Node<ShlInstr>.getInputLiveFlowCtx(): LiveFlowCtx {
    return instrs.first().liveFlowCtx
  }

  private fun Node<ShlInstr>.getOutputReachFlowCtx(): ReachFlowCtx {
    return instrs.last().reachFlowCtx
  }

  private fun Node<ShlInstr>.getOutputDataFlowCtx(): DataFlowCtx {
    return instrs.last().dataFlowCtx
  }
}

class LiveFlowCtx {
  companion object {
    private val tag = logTag(LiveFlowCtx::class)
  }

  private val varStates = mutableMapOf<String, MutableSet<InstrRef>>()

  fun processExpr(nodeIdx: Int, instrIdx: Int, writeExpr: ShlExpr?, readExpr: ShlExpr?) {
    writeExpr?.getUsedVars()?.forEach { writeVar ->
      varStates.remove(writeVar)
    }
    readExpr?.getUsedVars()?.forEach { readVar ->
      varStates.getOrPut(readVar, { mutableSetOf() }).apply {
        add(InstrRef(nodeIdx, instrIdx))
      }
    }
  }

  fun nullifyVars(nullifiedVars: Array<String>?) {
    if (nullifiedVars == null) return
    val varsToKill = mutableSetOf<String>()
    nullifiedVars.forEach { nullifiedVar ->
      varsToKill.add(nullifiedVar)
    }
    varsToKill.forEach { varStates.remove(it) }
  }

  fun clear() {
    varStates.clear()
  }

  fun get(variable: String): Set<InstrRef> {
    return varStates.getOrElse(variable, { emptySet() })
  }

  fun include(prevCtx: LiveFlowCtx) {
    prevCtx.varStates.forEach { varName, refs ->
      varStates.getOrPut(varName, { mutableSetOf() }).addAll(refs)
    }
  }

  fun dump(log: DecompLog) {
    log.trace(tag, "--- live flow ctx (where var is still alive) ---")
    varStates.forEach { varName, refs ->
      log.trace(tag, "$varName = ${refs.joinToString()}")
    }
  }

  fun join(other: LiveFlowCtx): LiveFlowCtx {
    val allVars = mutableSetOf<String>()
    allVars.addAll(varStates.keys)
    allVars.addAll(other.varStates.keys)
    allVars.forEach { varName ->
      val refs = mutableSetOf<InstrRef>()
      refs.addAll(varStates.getOrDefault(varName, mutableSetOf()))
      refs.addAll(other.varStates.getOrDefault(varName, mutableSetOf()))
      varStates[varName] = refs
    }
    return this
  }

  fun clone(): LiveFlowCtx {
    val newCtx = LiveFlowCtx()
    varStates.forEach { varName, refs ->
      val refsClone = mutableSetOf<InstrRef>()
      refsClone.addAll(refs)
      newCtx.varStates[varName] = refsClone
    }
    return newCtx
  }

  fun compare(other: LiveFlowCtx): Boolean {
    val allVars = mutableSetOf<String>()
    allVars.addAll(varStates.keys)
    allVars.addAll(other.varStates.keys)
    allVars.forEach { varName ->
      if (CollectionUtils.isEqualCollection(
          varStates.getOrDefault(varName, mutableSetOf()),
          other.varStates.getOrDefault(varName, mutableSetOf())
        ) == false
      ) {
        return false
      }
    }
    return true
  }
}

class ReachFlowCtx {
  companion object {
    private val tag = logTag(ReachFlowCtx::class)
  }

  private val varStates = mutableMapOf<String, MutableSet<InstrRef>>()

  fun processExpr(nodeIdx: Int, instrIdx: Int, writeExpr: ShlExpr?) {
    if (writeExpr == null) return
    writeExpr.getUsedVars().forEach { writeVar ->
      varStates.getOrPut(writeVar, { mutableSetOf() }).apply {
        clear()
        add(InstrRef(nodeIdx, instrIdx))
      }
    }
  }

  fun nullifyVars(nullifiedVars: Array<String>?) {
    if (nullifiedVars == null) return
    val varsToKill = mutableSetOf<String>()
    nullifiedVars.forEach { nullifiedVar ->
      varsToKill.add(nullifiedVar)
    }
    varsToKill.forEach { varStates.remove(it) }
  }

  fun clear() {
    varStates.clear()
  }

  fun get(variable: String): Set<InstrRef> {
    return varStates.getOrElse(variable, { emptySet() })
  }

  fun include(prevCtx: ReachFlowCtx) {
    prevCtx.varStates.forEach { varName, refs ->
      varStates.getOrPut(varName, { mutableSetOf() }).addAll(refs)
    }
  }

  fun dump(log: DecompLog) {
    log.trace(tag, "--- reach flow ctx (where var is defined)---")
    varStates.forEach { varName, refs ->
      log.trace(tag, "$varName = ${refs.joinToString()}")
    }
  }

  fun join(other: ReachFlowCtx): ReachFlowCtx {
    val allVars = mutableSetOf<String>()
    allVars.addAll(varStates.keys)
    allVars.addAll(other.varStates.keys)
    allVars.forEach { varName ->
      val refs = mutableSetOf<InstrRef>()
      refs.addAll(varStates.getOrDefault(varName, mutableSetOf()))
      refs.addAll(other.varStates.getOrDefault(varName, mutableSetOf()))
      varStates[varName] = refs
    }
    return this
  }

  fun clone(): ReachFlowCtx {
    val newCtx = ReachFlowCtx()
    varStates.forEach { varName, refs ->
      val refsClone = mutableSetOf<InstrRef>()
      refsClone.addAll(refs)
      newCtx.varStates[varName] = refsClone
    }
    return newCtx
  }

  fun compare(other: ReachFlowCtx): Boolean {
    val allVars = mutableSetOf<String>()
    allVars.addAll(varStates.keys)
    allVars.addAll(other.varStates.keys)
    allVars.forEach { varName ->
      if (CollectionUtils.isEqualCollection(
          varStates.getOrDefault(varName, mutableSetOf()),
          other.varStates.getOrDefault(varName, mutableSetOf())
        ) == false
      ) {
        return false
      }
    }
    return true
  }
}

class DataFlowCtx {
  companion object {
    private val tag = logTag(DataFlowCtx::class)
  }

  private val varStates = mutableMapOf<String, FlowState>()

  fun processExpr(writeExpr: ShlExpr?, readExpr: ShlExpr?, propagateKnownValues: Boolean) {
    if (writeExpr == null) return
    // local clone all states for variables used by readExpr
    val localStates = mutableMapOf<String, FlowState>()
    if (propagateKnownValues) {
      readExpr?.getUsedVars()?.forEach { readVar ->
        localStates[readVar] = varStates.getOrDefault(readVar, FlowState.BottomElement)
      }
    }

    // kill all states that contain variable from writeExpr
    writeExpr.getUsedVars().forEach { writeVar ->
      val varsToKill = mutableListOf<String>()
      varStates.forEach { varName, state ->
        if (state is FlowState.Alive && state.expr.getUsedVars().contains(writeVar)) {
          varsToKill.add(varName)
        }
      }
      varsToKill.forEach { varStates[it] = FlowState.TopElement }

    }
    // write new state for the variable used in writeExpr
    if (readExpr != null) {
      writeExpr.getUsedVars().forEach { writeVar ->
        var newReadExpr: ShlExpr = readExpr
        if (propagateKnownValues) {
          readExpr.getUsedVars().forEach { readVar ->
            if (localStates[readVar] is FlowState.Alive) {
              newReadExpr = newReadExpr.substitute(
                ShlExpr.ShlVar(readVar),
                false,
                (localStates[readVar] as FlowState.Alive).expr
              )
            }
          }
        }
        varStates[writeVar] = FlowState.Alive(newReadExpr)
      }
    }
  }

  fun nullifyVars(nullifiedVars: Array<String>?) {
    if (nullifiedVars == null) return
    val varsToKill = mutableSetOf<String>()
    nullifiedVars.forEach { nullifiedVar ->
      varStates.forEach { varName, state ->
        if (state is FlowState.Alive && state.expr.getUsedVars().contains(nullifiedVar)) {
          varsToKill.add(varName)
        }
      }
      if (varStates[nullifiedVar] != FlowState.TopElement) {
        varStates.remove(nullifiedVar)
      }
    }
    varsToKill.forEach { varStates[it] = FlowState.TopElement }
  }

  fun clear() {
    varStates.clear()
  }

  fun get(variable: String): FlowState {
    return varStates.getOrDefault(variable, FlowState.TopElement)
  }

  fun include(prevCtx: DataFlowCtx) {
    prevCtx.varStates.forEach { varName, state ->
      varStates[varName] = state
    }
  }

  fun join(other: DataFlowCtx): DataFlowCtx {
    val allVars = mutableSetOf<String>()
    allVars.addAll(varStates.keys)
    allVars.addAll(other.varStates.keys)
    allVars.forEach { varName ->
      varStates[varName] = varStates.getOrDefault(varName, FlowState.BottomElement)
        .join(other.varStates.getOrDefault(varName, FlowState.BottomElement))
    }
    return this
  }

  fun dump(log: DecompLog) {
    log.trace(tag, "--- data flow ctx ---")
    varStates.forEach { varName, state ->
      log.trace(tag, "$varName = $state")
    }
  }

  fun clone(): DataFlowCtx {
    val newCtx = DataFlowCtx()
    varStates.forEach { varName, state ->
      newCtx.varStates[varName] = state
    }
    return newCtx
  }

  fun compare(other: DataFlowCtx): Boolean {
    val allVars = mutableSetOf<String>()
    allVars.addAll(varStates.keys)
    allVars.addAll(other.varStates.keys)
    allVars.forEach { varName ->
      if (varStates.getOrDefault(varName, FlowState.BottomElement).compare(
          other.varStates.getOrDefault(varName, FlowState.BottomElement)
        ) == false
      ) {
        return false
      }
    }
    return true
  }
}

sealed class FlowState {
  class Alive(val expr: ShlExpr) : FlowState() {
    override fun join(other: FlowState): FlowState {
      when (other) {
        is Alive -> {
          if (expr.compareExpr(other.expr)) return this
          return TopElement
        }
        TopElement -> return TopElement
        BottomElement -> return this
      }
    }

    override fun compare(other: FlowState): Boolean {
      if (other !is Alive) return false
      return expr.compareExpr(other.expr)
    }

    override fun toString(): String {
      return "Alive(expr = '$expr') [expr.eval() = '${expr.evaluate()}']"
    }
  }

  object TopElement : FlowState() {
    override fun join(other: FlowState): FlowState {
      return TopElement
    }

    override fun compare(other: FlowState): Boolean {
      if (other !is TopElement) return false
      return true
    }

    override fun toString(): String {
      return "⊤"
    }
  }

  object BottomElement : FlowState() {
    override fun join(other: FlowState): FlowState {
      return other
    }

    override fun compare(other: FlowState): Boolean {
      if (other !is BottomElement) return false
      return true
    }

    override fun toString(): String {
      return "⊥"
    }
  }

  abstract fun join(other: FlowState): FlowState

  abstract fun compare(other: FlowState): Boolean
}
