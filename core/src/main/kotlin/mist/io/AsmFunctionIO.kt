/*
 * mist - interactive mips disassembler and decompiler
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

package mist.io

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Disposable
import com.google.gson.GsonBuilder
import kio.util.child
import kio.util.readJson
import kio.util.writeJson
import mist.asm.mips.EdgeKind
import mist.asm.mips.EdgeType
import mist.asm.mips.Graph
import mist.asm.mips.StackAnalysis
import mist.asm.mips.allegrex.AllegrexDisassembler
import mist.shl.ShlExpr
import mist.shl.ShlFunctionDef
import mist.shl.ShlGraph
import mist.shl.ShlInstr
import mist.util.DecompLog
import mist.util.logTag

/** @author Kotcrab */

class AsmFunctionIO(private val projectIO: ProjectIO, private val def: ShlFunctionDef, val log: DecompLog) :
    Disposable {
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
        log.info(tag, "performing initial disassembly")
        val disassembly = AllegrexDisassembler().disassemble(projectIO.getElfLoader(), def.toLLDef())
        val graph = Graph(projectIO.getElfLoader(), disassembly.instr, log)
        graph.generateGraph()
        val stack = StackAnalysis(graph, log)
        stack.analyze()
        val shlGraph = ShlGraph(projectIO, def, log)
        shlGraph.generateGraph(graph, stack)
        return LoadedFunc(shlGraph, null)
    }

    private fun loadFile(): LoadedFunc {
        log.info(tag, "loading disassembly of ${def.name}")
        val funcDto: FuncDto = funcFile.readJson(gson)
        val shlGraph = ShlGraph(projectIO, def, log)
        shlGraph.loadGraph(funcDto.nodes)
        return LoadedFunc(shlGraph, funcDto.nodes.map { Vector2(it.x, it.y) })
    }

    fun save(func: LoadedFunc) {
        if (func.layoutData == null || func.layoutData.size != func.graph.nodes.size) {
            log.panic(tag, "layout data missing or invalid when saving function")
        }
        log.info(tag, "saving disassembly of ${def.name}")
        val nodes = func.graph.nodes
        val nodesDto = nodes.mapIndexed { nodeIdx, node ->
            val edgesDto = node.outEdges.map { edge ->
                EdgeDto(nodes.indexOf(edge.node), edge.type, edge.kind)
            }
            NodeDto(node.instrs, func.layoutData[nodeIdx].x, func.layoutData[nodeIdx].y, edgesDto)
        }
        funcFile.writeJson(gson, FuncDto(nodesDto))
        log.trace(tag, "committing version to local history")
        localHistory.commit(funcFile.readBytes())
    }

    fun revert(entry: LocalHistoryEntry) {
        funcFile.writeBytes(localHistory.read(entry))
    }

    fun delete() {
        funcFile.delete()
    }

    override fun dispose() {
        localHistory.dispose()
    }

    fun getLocalHistoryEntries(): LocalHistory.EntryReader {
        return localHistory.entryReader()
    }
}

class LoadedFunc(val graph: ShlGraph, val layoutData: List<Vector2>?)

class FuncDto(val nodes: List<NodeDto>)

class NodeDto(val instr: List<ShlInstr>, val x: Float, val y: Float, val outEdges: List<EdgeDto>)

class EdgeDto(val node: Int, val type: EdgeType, val kind: EdgeKind)
