package mist.io

import com.google.gson.GsonBuilder
import kio.util.child
import kio.util.readJson
import kio.util.writeJson
import mist.asm.mips.EdgeKind
import mist.asm.mips.EdgeType
import mist.asm.mips.MipsGraph
import mist.asm.mips.MipsStackAnalysis
import mist.asm.mips.allegrex.AllegrexDisassembler
import mist.shl.ShlExpr
import mist.shl.ShlFunctionDef
import mist.shl.ShlGraph
import mist.shl.ShlInstr
import mist.util.MistLogger
import mist.util.Point
import mist.util.logTag

class AsmFunctionIO(private val projectIO: ProjectIO, private val def: ShlFunctionDef, val logger: MistLogger) {
  private val tag = logTag()
  private val gson = GsonBuilder()
    .setPrettyPrinting()
    .registerTypeAdapterFactory(ShlExpr.provideGsonTypeAdapter())
    .registerTypeAdapterFactory(ShlInstr.provideGsonTypeAdapter())
    .create()

  private val funcDir = projectIO.getFuncDir(def)
  private val funcFile = funcDir.child("shl.json")
  private val localHistory = LocalHistory(funcDir)

  fun load(ignoreSaved: Boolean) = if (funcFile.exists() && ignoreSaved == false) loadFile() else initialDisassemble()

  private fun initialDisassemble(): LoadedFunc {
    logger.info(tag, "performing initial disassembly")
    val disassembly = AllegrexDisassembler().disassemble(projectIO.getElfLoader(), def.toLLDef())
    val graph = MipsGraph(projectIO.getElfLoader(), disassembly.instrs, logger)
    graph.generateGraph()
    val stack = MipsStackAnalysis(graph, logger)
    stack.analyze()
    val shlGraph = ShlGraph(projectIO, def, logger)
    shlGraph.generateGraph(graph, stack)
    return LoadedFunc(shlGraph, null)
  }

  private fun loadFile(): LoadedFunc {
    logger.info(tag, "loading disassembly of ${def.name}")
    val funcDto: FuncDto = funcFile.readJson(gson)
    val shlGraph = ShlGraph(projectIO, def, logger)
    shlGraph.loadGraph(funcDto.nodes)
    return LoadedFunc(shlGraph, funcDto.nodes.map { Point(it.x, it.y) })
  }

  fun save(func: LoadedFunc) {
    if (func.layoutData == null || func.layoutData.size != func.graph.nodes.size) {
      logger.panic(tag, "layout data missing or invalid when saving function")
    }
    logger.info(tag, "saving disassembly of ${def.name}")
    val nodes = func.graph.nodes
    val nodesDto = nodes.mapIndexed { nodeIdx, node ->
      val edgesDto = node.outEdges.map { edge ->
        EdgeDto(nodes.indexOf(edge.node), edge.type, edge.kind)
      }
      NodeDto(node.instrs, func.layoutData[nodeIdx].x, func.layoutData[nodeIdx].y, edgesDto)
    }
    funcFile.writeJson(gson, FuncDto(nodesDto))
    logger.trace(tag, "committing version to local history")
    localHistory.commit(funcFile.readBytes())
  }

  fun revert(entry: LocalHistoryEntry) {
    funcFile.writeBytes(localHistory.read(entry))
  }

  fun delete() {
    funcFile.delete()
  }

  fun dispose() {
    localHistory.dispose()
  }

  fun getLocalHistoryEntries(): LocalHistory.EntryReader {
    return localHistory.entryReader()
  }
}

class LoadedFunc(val graph: ShlGraph, val layoutData: List<Point>?)

class FuncDto(val nodes: List<NodeDto>)

class NodeDto(val instr: List<ShlInstr>, val x: Float, val y: Float, val outEdges: List<EdgeDto>)

class EdgeDto(val node: Int, val type: EdgeType, val kind: EdgeKind)
