package mist.shl

import mist.asm.mips.EdgeKind
import mist.asm.mips.EdgeType
import mist.asm.mips.Node
import mist.shl.ShlExpr.*
import mist.util.MistLogger
import mist.util.logTag

class ExprMutator(val graph: ShlGraph, private val logger: MistLogger) {
  val tag = logTag()

  fun renameAssignedVariable(instr: ShlInstr, newName: String) {
    val writeExpr = instr.getWriteExpr()
    if (writeExpr == null) {
      logger.fatal(tag, "renamed instruction does not return write expression")
      return
    }
    val writeVars = writeExpr.getUsedVars()
    if (writeVars.size != 1) {
      logger.fatal(tag, "write expression must specify exactly one write variable")
      return
    }
    val writeVar = writeVars[0]
    logger.info(tag, "rename $writeVar -> $newName")
    val oldVar = ShlExpr.ShlVar(writeVar)
    val newVar = ShlExpr.ShlVar(newName)
    renameAssignedVariable(instr, oldVar, newVar, mutableSetOf(), mutableSetOf())
  }

  private fun renameAssignedVariable(
    instr: ShlInstr, oldVar: ShlExpr.ShlVar, newVar: ShlExpr.ShlVar, processedUses: MutableSet<ShlInstr>,
    processedAssigns: MutableSet<ShlInstr>
  ) {
    val discoveredAssigns = mutableSetOf<ShlInstr>()
    logger.trace(tag, "rename in assign '$instr'")
    instr.substituteWriteExpr(oldVar, false, newVar)
    processedAssigns.add(instr)
    val liveRefs = instr.nextLiveFlowCtx.get(oldVar.varName)
    logger.info(tag, "rename variable in max. ${liveRefs.size} uses")
    liveRefs.forEach propagateTarget@{ liveRef ->
      val liveInstr = graph.getInstr(liveRef)
      liveInstr.prevReachFlowCtx.get(oldVar.varName).forEach reachCheck@{ reachRef ->
        val reachInstr = graph.getInstr(reachRef)
        discoveredAssigns.add(reachInstr)
      }
      if (liveInstr !in processedUses) {
        logger.trace(tag, "rename in '$liveInstr'")
        liveInstr.substituteReadExpr(oldVar, false, newVar)
        processedUses.add(liveInstr)
      }
    }
    logger.info(tag, "rename in usages finished, rename in discovered assigns")
    discoveredAssigns.forEach { assignInstr ->
      if (assignInstr !in processedAssigns) {
        logger.trace(tag, "recursive rename from '$assignInstr'")
        renameAssignedVariable(assignInstr, oldVar, newVar, processedUses, processedAssigns)
      }
    }
  }

  fun propagate(instr: ShlInstr) {
    if (instr !is ShlAssignInstr) {
      logger.fatal(tag, "can only propagate assign instruction")
      return
    }
    val writeExpr = instr.getWriteExpr()
    if (writeExpr == null) {
      logger.fatal(tag, "propagate instruction does not return write expression")
      return
    }
    val writeVars = writeExpr.getUsedVars()
    if (writeVars.size != 1) {
      logger.fatal(tag, "write expression must specify exactly one write variable")
      return
    }
    val writeVar = writeVars[0]
    val expectedState = instr.dataFlowCtx.get(writeVar)
    if (expectedState !is FlowState.Alive) {
      logger.fatal(tag, "write variable starting state is not alive")
      return
    }
    val discoveredAssigns = mutableSetOf<ShlInstr>()
    discoveredAssigns.add(instr)

    val liveRefs = instr.nextLiveFlowCtx.get(writeVar)
    logger.info(tag, "propagate variable $writeVar from '$instr' to ${liveRefs.size} instructions")
    var fullyPropagated = true
    var propagationsPerformed = 0
    liveRefs.forEach propagateTarget@{ liveRef ->
      // check if propagation safe for reference
      val liveInstr = graph.getInstr(liveRef)
      val stateMatches = liveInstr.prevDataFlowCtx.get(writeVar).compare(expectedState)
      if (stateMatches == false) {
        logger.trace(tag, "state mismatch, can't propagate to '$liveInstr'")
        fullyPropagated = false
        return@propagateTarget
      }

      liveInstr.prevReachFlowCtx.get(writeVar).forEach reachCheck@{ reachRef ->
        val reachInstr = graph.getInstr(reachRef)
        val assignMatches = reachInstr.dataFlowCtx.get(writeVar).compare(expectedState)
        if (assignMatches == false) {
          logger.trace(tag, "reach assign state mismatch, can't propagate to' $liveInstr'")
          fullyPropagated = false
          return@propagateTarget
        }
        discoveredAssigns.add(reachInstr)
      }

      logger.trace(tag, "propagate to '$liveInstr'")
      liveInstr.substituteReadExpr(ShlExpr.ShlVar(writeVar), false, expectedState.expr)
      propagationsPerformed++
    }
    if (fullyPropagated) {
      logger.info(tag, "propagation successful")
      discoveredAssigns.forEach {
        logger.trace(tag, "marking $it as dead code")
        it.deadCode = true
      }
    } else {
      logger.warn(tag, "full propagation was impossible")
      if (propagationsPerformed > 0) {
        discoveredAssigns.forEach {
          val msg = "(partially propagated)"
          if (it.comment.contains(msg)) return@forEach
          it.comment += msg
        }
      }
    }
  }

  fun evaluate(instr: ShlInstr) {
    when (instr) {
      is ShlAssignInstr -> {
        instr.src = instr.src.evaluate()
      }
      is ShlBranchInstr -> {
        instr.cond = instr.cond.evaluate()
      }
      is ShlReturnInstr -> {
        instr.retVal?.let { retVal ->
          instr.retVal = retVal.evaluate()
        }
      }
      is ShlMemStoreInstr -> {
        instr.expr = instr.expr.evaluate() as ShlMemStore
      }
      is ShlCallInstr -> {
        instr.callExpr = instr.callExpr.evaluate()
      }
      else -> logger.warn(tag, "evaluating $instr not implemented")
    }
  }

  fun simplifyCondition(instr: ShlInstr) {
    if (instr !is ShlBranchInstr) {
      logger.fatal(tag, "can't simplify non branch instruction")
      return
    }
    val cond = instr.cond
    if (cond !is ShlExpr.ShlBinaryExpr) {
      logger.fatal(tag, "condition is not binary expression, can't simplify")
      return
    }
    if (cond.right !is ShlExpr.ShlConst) {
      logger.fatal(tag, "right condition side is not constant, can't simplify")
      return
    }
    if (cond.left !is ShlExpr.ShlEquality) {
      logger.fatal(tag, "left condition side is not equality operator, can't simplify")
      return
    }
    val right = cond.right
    val left = cond.left as ShlExpr.ShlEquality
    when (cond) {
      is ShlExpr.ShlEqual -> {
        var newCond: ShlExpr = cond.left
        // negation needed?
        if (right.value == 0) {
          newCond = left.negate()
        }
        // flip needed?
        if (newCond is ShlExpr.ShlEquality && newCond is ShlExpr.ShlBinaryExpr
          && newCond.left is ShlExpr.ShlConst && newCond.right is ShlExpr.ShlVar
        ) {
          newCond = newCond.flip()
        }
        instr.cond = newCond
        logger.info(tag, "condition simplified")
      }
      is ShlExpr.ShlNotEqual -> {
        var newCond: ShlExpr = cond.left
        // negation needed?
        if (right.value != 0) {
          newCond = left.negate()
        }
        // flip needed?
        if (newCond is ShlExpr.ShlEquality && newCond is ShlExpr.ShlBinaryExpr
          && newCond.left is ShlExpr.ShlConst && newCond.right is ShlExpr.ShlVar
        ) {
          newCond = newCond.flip()
        }
        instr.cond = newCond
        logger.info(tag, "condition simplified")

      }
      else -> logger.warn(tag, "unsupported condition case")
    }
  }

  fun flipBranch(instr: ShlInstr) {
    if (instr !is ShlBranchInstr) {
      logger.fatal(tag, "can't flip non branch instruction")
      return
    }
    val cond = instr.cond
    if (cond !is ShlExpr.ShlEquality) {
      logger.fatal(tag, "can't flip non equality condition")
      return
    }
    val instrNode = graph.getNodeForInstr(instr)
    val outEdges = instrNode.outEdges
    if (outEdges.size != 2) {
      logger.fatal(tag, "node does not have 2 out edges, can't flip")
      return
    }
    val jumpTaken = instrNode.outEdges.firstOrNull { it.type == EdgeType.JumpTaken }
    val fallthrough = instrNode.outEdges.firstOrNull { it.type == EdgeType.Fallthrough }
    if (jumpTaken == null || fallthrough == null) {
      logger.fatal(tag, "node does not have jump taken and fallthrough edge, can't flip")
      return
    }
    if (jumpTaken.kind == EdgeKind.BackEdge || fallthrough.kind == EdgeKind.BackEdge) {
      logger.fatal(tag, "node has back edges, flipping might be unsafe and won't be performed")
      return
    }
    instr.cond = cond.negate()
    instrNode.removeNode(jumpTaken)
    instrNode.removeNode(fallthrough)
    instrNode.addNode(jumpTaken.node, fallthrough.type, fallthrough.kind)
    instrNode.addNode(fallthrough.node, jumpTaken.type, jumpTaken.kind)
    logger.info(tag, "condition flipped")
  }

  fun eliminateBranch(instr: ShlInstr) {
    if (instr !is ShlBranchInstr) {
      logger.fatal(tag, "can't eliminate non branch instruction")
      return
    }
    val evalCond = instr.cond.evaluate()
    if (evalCond !is ShlExpr.ShlConst) {
      logger.fatal(tag, "branch condition does not evaluate to constant, got: '$evalCond'")
      return
    }
    val instrNode = graph.getNodeForInstr(instr)
    if (instrNode.outEdges.size != 2) {
      logger.fatal(tag, "node has got more than two out edges, can't eliminate branch")
      return
    }
    val edgeTypeToRemove = if (evalCond.value == 0) {
      EdgeType.JumpTaken
    } else {
      EdgeType.Fallthrough
    }
    val nodesToFullyRemove = mutableListOf<Node<ShlInstr>>()
    instrNode.outEdges.filter { it.type == edgeTypeToRemove }.forEach { outEdge ->
      instrNode.removeNode(outEdge)
      if (outEdge.node.inEdges.size == 0) {
        nodesToFullyRemove.add(outEdge.node)
      }
    }
    instr.deadCode = true
    logger.info(tag, "branch eliminated")
    nodesToFullyRemove.forEach { nodeToRemove ->
      graph.removeNode(nodeToRemove)
    }
  }

  fun optimizeGraph() {
    graph.optimizeNodes()
  }

  fun mutateToStructAccess(instr: ShlInstr, struct: ShlType.ShlStruct) {
    val node = graph.getNodeForInstr(instr)
    val instrIdx = node.instrs.indexOf(instr)
    if (instr is ShlMemStoreInstr) {
      val store = ShlStructStore(instr.expr.op, struct.tid, instr.expr.memExpr, instr.expr.valExpr)
      node.instrs[instrIdx] = ShlStructStoreInstr(instr.addr, store)
      node.instrs[instrIdx].comment = instr.comment
      logger.info(tag, "mutated to struct store")
    } else if (instr is ShlAssignInstr && instr.src is ShlMemLoad) {
      val oldLoad = instr.src as ShlMemLoad
      val load = ShlStructLoad(oldLoad.op, struct.tid, oldLoad.expr)
      node.instrs[instrIdx] = ShlAssignInstr(instr.addr, instr.dest, load)
      node.instrs[instrIdx].comment = instr.comment
      logger.info(tag, "mutated to struct load")
    } else {
      logger.warn(tag, "can't mutate this instruction to use struct access")
      return
    }
  }

  fun mutateToMemoryAccess(instr: ShlInstr) {
    val node = graph.getNodeForInstr(instr)
    val instrIdx = node.instrs.indexOf(instr)
    if (instr is ShlStructStoreInstr) {
      val store = ShlMemStore(instr.expr.op, instr.expr.memExpr, instr.expr.valExpr)
      node.instrs[instrIdx] = ShlMemStoreInstr(instr.addr, store)
      node.instrs[instrIdx].comment = instr.comment
      logger.info(tag, "mutated to memory store")
    } else if (instr is ShlAssignInstr && instr.src is ShlStructLoad) {
      val oldLoad = instr.src as ShlStructLoad
      val load = ShlMemLoad(oldLoad.op, oldLoad.expr)
      node.instrs[instrIdx] = ShlAssignInstr(instr.addr, instr.dest, load)
      node.instrs[instrIdx].comment = instr.comment
      logger.info(tag, "mutated to memory load")
    } else {
      logger.warn(tag, "can't mutate this instruction to use memory access")
      return
    }
  }

  fun removeReturnValue(instr: ShlInstr) {
    if (instr !is ShlReturnInstr) {
      logger.fatal(tag, "can't remove return value on non return instruction")
      return
    }
    instr.retVal = null
    val node = graph.getNodeForInstr(instr)
    val instrIdx = node.instrs.indexOf(instr)
    if (instrIdx == node.instrs.lastIndex && node.outEdges.size == 0) {
      instr.deadCode = true
    }
  }

  fun addReturnValue(instr: ShlInstr, expr: ShlExpr) {
    if (instr !is ShlReturnInstr) {
      logger.fatal(tag, "can't add return value on non return instruction")
      return
    }
    instr.retVal = expr
    instr.deadCode = false
  }
}
