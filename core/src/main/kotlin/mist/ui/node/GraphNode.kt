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

package mist.ui.node

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.kotcrab.vis.ui.util.dialog.Dialogs
import com.kotcrab.vis.ui.util.dialog.InputDialogAdapter
import com.kotcrab.vis.ui.widget.PopupMenu
import com.kotcrab.vis.ui.widget.VisLabel
import kio.util.toHex
import kio.util.toWHex
import ktx.actors.onChange
import ktx.inject.Context
import ktx.vis.menuItem
import ktx.vis.popupMenu
import ktx.vis.subMenu
import mist.asm.EdgeKind
import mist.asm.EdgeType
import mist.asm.Node
import mist.io.ProjectIO
import mist.shl.*
import mist.shl.ShlExpr.*
import mist.shl.ShlType.ShlStruct
import mist.ui.dialog.TextAreaInputDialog
import mist.util.DecompLog

/** @author Kotcrab */

class GraphNode(context: Context,
                nodeStage: Stage,
                private val exprMutator: ExprMutator,
                xPos: Float,
                yPos: Float,
                val node: Node<ShlInstr>,
                nodeIndex: Int,
                private val listener: Listener,
                titleDef: ShlFunctionDef? = null,
                val onFuncDoubleClick: (ShlFunctionDef) -> Unit) : VisualNode(context, Color.WHITE, xPos, yPos) {
    private val tag = javaClass.simpleName
    private val appStage: Stage = context.inject()
    private val projectIO: ProjectIO = context.inject()
    private val log: DecompLog = context.inject()
    private val types = projectIO.getTypes()
    private val globals = projectIO.getGlobals()

    private val inEdges = mutableListOf<CodeNodeEdge>()
    private val outEdges = mutableListOf<CodeNodeEdge>()

    private val dbgLabels = mutableMapOf<Int, CodeLabel>()
    private val highlighted = mutableListOf<CodeLabel>()

    private val blueSyntaxColor = Color(190f / 255f, 214f / 255f, 255f / 255f, 1f)
    private val darkBlueSyntaxColor = Color(121f / 255f, 175f / 255f, 255f / 255f, 1f)
    private val greenSyntaxColor = Color(127f / 255f, 179f / 255f, 71f / 255f, 1f)
    private val yellowSyntaxColor = Color(217f / 255f, 229f / 255f, 119f / 255f, 1f)
    private val orangeSyntaxColor = Color(239f / 255f, 192f / 255f, 144f / 255f, 1f)
    private val redSyntaxColor = Color(188f / 255f, 63f / 255f, 60f / 255f, 1f)
    private val highlightSyntaxColor = Color.RED

    init {
        table.left()
        table.defaults().left()
        table.add(VisLabel("$nodeIndex: ${titleDef?.toString() ?: ""}", labelStyle)).row()
        node.instrs.forEachIndexed { instrIdx, instr ->
            val line = HorizontalGroup()
            renderInstr(instrIdx, instr, line)
            table.add(line).row()
        }
        nodeStage.addActor(table)
        updateBounds()
    }

    override fun updateBounds() {
        val width = table.prefWidth
        val height = table.prefHeight
        bounds.setSize(width, height)
        table.setPosition(bounds.x, bounds.y + bounds.height / 2)
        val conSize = 6f
        inConBounds.set(getX() + width / 2, getY() + height - conSize, conSize, conSize)
        outConBounds.set(getX() + width / 2, getY(), conSize, conSize)
        // discard extra edge data on drag for now
        outEdges.forEach { it.points = null }
    }

    fun addNode(otherNode: GraphNode, edgeType: EdgeType, edgeKind: EdgeKind) {
        outEdges.add(CodeNodeEdge(otherNode, edgeType, edgeKind))
        otherNode.inEdges.add(CodeNodeEdge(this, edgeType, edgeKind))
    }

    fun getOutEdges(): List<CodeNodeEdge> = outEdges
    fun getInEdges(): List<CodeNodeEdge> = inEdges

    private fun renderInstr(instrIdx: Int, instr: ShlInstr, line: HorizontalGroup) {
        val dbgLabel = line.asmLabel(instr, "->")
        dbgLabel.isVisible = false
        dbgLabels[instr.addr] = dbgLabel
        line.asmLabel(instr, "$instrIdx", blueSyntaxColor)
        line.asmMiscLabel(instr, " ")
        line.asmLabel(instr, "@${instr.addr.toWHex()}", blueSyntaxColor)
        line.asmMiscLabel(instr, " ")
        when (instr) {
            is ShlAssignInstr -> renderAssignInstr(line, instr)
            is ShlBranchInstr -> renderBranchInstr(line, instr)
            is ShlJumpInstr -> renderJumpInstr(line, instr)
            is ShlCallInstr -> renderCallInstr(line, instr)
            is ShlMemStoreInstr -> renderExpr(line, instr, instr.expr)
            is ShlStructStoreInstr -> renderExpr(line, instr, instr.expr)
            is ShlNopInstr -> line.asmLabel(instr, instr.toString())
            else -> log.panic(tag, "missing ShlInstr renderer")
        }
        if (instr.comment.isNotBlank()) {
            line.asmMiscLabel(instr, " # ", yellowSyntaxColor)
            line.asmMiscLabel(instr, instr.comment, yellowSyntaxColor)
        }
    }

    private fun renderAssignInstr(line: HorizontalGroup, instr: ShlAssignInstr) {
        renderExpr(line, instr, instr.dest)
        line.asmMiscLabel(instr, " = ")
        renderExpr(line, instr, instr.src)
    }

    private fun renderBranchInstr(line: HorizontalGroup, instr: ShlBranchInstr) {
        //${if (instr.likely) "l" else ""} not including likely, not needed with branch delay slot transformation
        line.asmLabel(instr, "b${if (instr.link) "al" else ""}")
        line.asmMiscLabel(instr, " ")
        renderExpr(line, instr, instr.cond)
    }

    private fun renderJumpInstr(line: HorizontalGroup, instr: ShlJumpInstr) {
        val jalDef = if (instr.dest is ShlConst) {
            projectIO.getFuncDefByOffset((instr.dest as ShlConst).value)
        } else null
        if (jalDef != null && jalDef.returnType == "void") {
            line.asmLabel(instr, "v0", blueSyntaxColor)
            line.asmMiscLabel(instr, " = ")
        }
        line.asmLabel(instr, "j${if (instr.link) "al" else ""}")
        line.asmMiscLabel(instr, " ")
        if (jalDef != null) {
            line.asmLabel(instr, jalDef.toCallString(), blueSyntaxColor)
        } else {
            renderExpr(line, instr, instr.dest)
        }
    }

    private fun renderCallInstr(line: HorizontalGroup, instr: ShlCallInstr) {
        instr.returnReg?.let {
            renderExpr(line, instr, it)
            line.asmMiscLabel(instr, " = ")
        }
        renderExpr(line, instr, instr.callExpr)
    }

    private fun renderExpr(target: HorizontalGroup, relInstr: ShlInstr, expr: ShlExpr) {
        when (expr) {
            is ShlVar -> target.asmExprLabel(relInstr, expr, blueSyntaxColor)
            is ShlConst -> target.asmExprLabel(relInstr, expr, greenSyntaxColor)
            is ShlFloat -> target.asmExprLabel(relInstr, expr, greenSyntaxColor)
            is ShlString -> target.asmExprLabel(relInstr, expr, expr.toExprString().replace("\n", "\\n"), yellowSyntaxColor)
            is ShlGlobalRef -> renderGlobalRefExpr(target, relInstr, expr)
            is ShlFuncPointer -> renderFuncPointerExpr(target, relInstr, expr)
            is ShlUnaryExpr -> renderUnaryExpr(target, relInstr, expr)
            is ShlBinaryExpr -> renderBinaryExpr(target, relInstr, expr)
            is ShlMemLoad -> renderMemLoadExpr(target, relInstr, expr)
            is ShlMemStore -> renderMemStoreExpr(target, relInstr, expr)
            is ShlStructLoad -> renderStructLoadExpr(target, relInstr, expr)
            is ShlStructStore -> renderStructStoreExpr(target, relInstr, expr)
            is ShlCall -> renderCallExpr(target, relInstr, expr)
            else -> log.panic(tag, "missing ShlExpr renderer")
        }
    }

    private fun renderGlobalRefExpr(target: HorizontalGroup, relInstr: ShlInstr, expr: ShlGlobalRef) {
        val global = globals.getGlobalByAddress(expr.addr)
        if (global == null) {
            target.asmExprLabel(relInstr, expr, "((__global_fail)${expr.addr.toWHex()})", redSyntaxColor)
        } else {
            target.asmExprLabel(relInstr, expr, global.name, orangeSyntaxColor)
        }
    }

    private fun renderFuncPointerExpr(target: HorizontalGroup, relInstr: ShlInstr, expr: ShlFuncPointer) {
        val jalDef = projectIO.getFuncDefByOffset(expr.addr)
                ?: log.panic(tag, "failed to find function definition for ShlFunctionPointer addr: ${expr.addr}")
        target.asmExprLabel(relInstr, expr, jalDef.name, darkBlueSyntaxColor)
    }

    private fun renderUnaryExpr(target: HorizontalGroup, relInstr: ShlInstr, expr: ShlUnaryExpr) {
        if (expr.wrapParenthesis) {
            val childTarget = HorizontalGroup()
            childTarget.asmMiscLabel(relInstr, expr.op)
            childTarget.asmMiscLabel(relInstr, "(")
            renderExpr(childTarget, relInstr, expr.expr)
            childTarget.asmMiscLabel(relInstr, ")")
            target.addActor(childTarget)
        } else {
            target.asmMiscLabel(relInstr, expr.op)
            renderExpr(target, relInstr, expr.expr)
        }
    }

    private fun renderBinaryExpr(target: HorizontalGroup, relInstr: ShlInstr, expr: ShlBinaryExpr) {
        val childTarget = HorizontalGroup()
        if (expr.left is ShlCompoundExpr) {
            childTarget.asmMiscLabel(relInstr, "(")
            renderExpr(childTarget, relInstr, expr.left)
            childTarget.asmMiscLabel(relInstr, ")")
        } else {
            renderExpr(childTarget, relInstr, expr.left)
        }
        childTarget.asmMiscLabel(relInstr, " ${expr.op} ")
        if (expr.right is ShlCompoundExpr) {
            childTarget.asmMiscLabel(relInstr, "(")
            renderExpr(childTarget, relInstr, expr.right)
            childTarget.asmMiscLabel(relInstr, ")")
        } else {
            renderExpr(childTarget, relInstr, expr.right)
        }
        target.addActor(childTarget)
    }

    private fun renderMemStoreExpr(target: HorizontalGroup, relInstr: ShlInstr, expr: ShlMemStore) {
        val childTarget = HorizontalGroup()
        childTarget.asmMiscLabel(relInstr, expr.op.toString().toLowerCase())
        childTarget.asmMiscLabel(relInstr, "[")
        renderExpr(childTarget, relInstr, expr.memExpr)
        childTarget.asmMiscLabel(relInstr, "]")
        childTarget.asmMiscLabel(relInstr, " = ")
        renderExpr(childTarget, relInstr, expr.valExpr)
        target.addActor(childTarget)
    }

    private fun renderMemLoadExpr(target: HorizontalGroup, relInstr: ShlInstr, expr: ShlMemLoad) {
        val childTarget = HorizontalGroup()
        childTarget.asmMiscLabel(relInstr, expr.op.toString().toLowerCase())
        childTarget.asmMiscLabel(relInstr, "[")
        renderExpr(childTarget, relInstr, expr.expr)
        childTarget.asmMiscLabel(relInstr, "]")
        target.addActor(childTarget)
    }

    private fun renderStructStoreExpr(target: HorizontalGroup, relInstr: ShlInstr, expr: ShlStructStore) {
        val childTarget = HorizontalGroup()
        val (memBase, memOffset) = getStructAccess(expr.memExpr)
        val memAccessSize = expr.op.accessSize
        val struct = types.getType(expr.refTid) as? ShlStruct ?: error("refTid is not a struct")
        val (field, accessString) = types.getAccessedStructField(struct, memOffset.value)
        if (field == null) {
            log.warn(tag, "struct store fallback render used")
            childTarget.asmLabel(relInstr, "__struct_fail ", redSyntaxColor)
            childTarget.asmMiscLabel(relInstr, expr.op.toString().toLowerCase())
            childTarget.asmMiscLabel(relInstr, "[")
            renderExpr(childTarget, relInstr, expr.memExpr)
            childTarget.asmMiscLabel(relInstr, "]")
            childTarget.asmMiscLabel(relInstr, " = ")
            renderExpr(childTarget, relInstr, expr.valExpr)
            target.addActor(childTarget)
        } else {
            val opNeeded = types.sizeOfStructArrayField(field) != memAccessSize
            if (opNeeded) {
                childTarget.asmLabel(relInstr, "_${expr.op.toString().toLowerCase()} ", redSyntaxColor)
            }
            childTarget.asmLabel(relInstr, struct.name)
            childTarget.asmMiscLabel(relInstr, "(")
            renderExpr(childTarget, relInstr, memBase)
            childTarget.asmMiscLabel(relInstr, ").")
            childTarget.asmLabel(relInstr, accessString)
            childTarget.asmMiscLabel(relInstr, " = ")
            renderExpr(childTarget, relInstr, expr.valExpr)
            target.addActor(childTarget)
        }
    }

    private fun renderStructLoadExpr(target: HorizontalGroup, relInstr: ShlInstr, expr: ShlStructLoad) {
        val childTarget = HorizontalGroup()
        val (memBase, memOffset) = getStructAccess(expr.expr)
        val memAccessSize = expr.op.accessSize
        val struct = types.getType(expr.refTid) as? ShlStruct ?: error("refTid is not a struct")
        val (field, accessString) = types.getAccessedStructField(struct, memOffset.value)
        if (field == null) {
            log.warn(tag, "struct load fallback render used")
            childTarget.asmLabel(relInstr, "__struct_fail ", redSyntaxColor)
            childTarget.asmMiscLabel(relInstr, expr.op.toString().toLowerCase())
            childTarget.asmMiscLabel(relInstr, "[")
            renderExpr(childTarget, relInstr, expr.expr)
            childTarget.asmMiscLabel(relInstr, "]")
            target.addActor(childTarget)
        } else {
            val opNeeded = types.sizeOfStructArrayField(field) != memAccessSize
            if (opNeeded) {
                childTarget.asmLabel(relInstr, "_${expr.op.toString().toLowerCase()} ", redSyntaxColor)
            }
            childTarget.asmLabel(relInstr, struct.name)
            childTarget.asmMiscLabel(relInstr, "(")
            renderExpr(childTarget, relInstr, memBase)
            childTarget.asmMiscLabel(relInstr, ").")
            childTarget.asmLabel(relInstr, accessString)
            target.addActor(childTarget)
        }
    }

    private fun getStructAccess(memExpr: ShlExpr): Pair<ShlExpr, ShlConst> {
        val memBase: ShlExpr
        val memOffset: ShlConst
        if (memExpr is ShlAdd && memExpr.right is ShlConst) {
            memBase = memExpr.left
            memOffset = memExpr.right
        } else {
            memBase = memExpr
            memOffset = ShlConst(0x0)
        }
        return Pair(memBase, memOffset)
    }

    private fun renderCallExpr(target: HorizontalGroup, relInstr: ShlInstr, expr: ShlCall) {
        val childTarget = HorizontalGroup()
        val jalDef = projectIO.getFuncDefByOffset(expr.dest)
                ?: log.panic(tag, "failed to find function definition for ShlCallInstr destination ${expr.dest}")

        val nameLabel = childTarget.asmLabel(relInstr, jalDef.name)
        nameLabel.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (tapCount == 2) {
                    onFuncDoubleClick(jalDef)
                }
            }
        })

        val argsToRender = mutableListOf<Triple<String, ShlExpr, Color>>()
        val remainExprArgs = expr.args.toMutableMap()
        // fill args to render
        val freeArgs = shlArgRegisters.toMutableList()
        jalDef.arguments.forEach { jalArg ->
            if (freeArgs.remove(jalArg.register) == false) log.panic(tag, "conflicting func. arg definitions (free arg list exhausted)")
            val hasArg = expr.args.contains(jalArg.register)
            if (jalArg.type.endsWith("...")) {
                argsToRender.add(Triple("${jalArg.name}[0]", expr.args.getOrElse(jalArg.register, { ShlVar(jalArg.register) }),
                        if (hasArg) Color.WHITE else Color.RED))
                remainExprArgs.remove(jalArg.register)
                freeArgs.forEachIndexed { freeArgIdx, freeArg ->
                    // only display free arg if some unique value is assigned
                    val hasVararg = expr.args.contains(jalArg.register)
                    if (expr.args[freeArg]?.compareExpr(ShlVar(freeArg)) == false) {
                        argsToRender.add(Triple("${jalArg.name}[${freeArgIdx + 1}]", expr.args.getOrElse(freeArg, { ShlVar(freeArg) }),
                                if (hasVararg) Color.WHITE else Color.RED))
                    }
                    remainExprArgs.remove(freeArg)
                }
            } else {
                argsToRender.add(Triple(jalArg.name, expr.args.getOrElse(jalArg.register, { ShlVar(jalArg.register) }),
                        if (hasArg) Color.WHITE else Color.RED))
                remainExprArgs.remove(jalArg.register)
            }
        }
        remainExprArgs.forEach { regName, argExpr ->
            argsToRender.add(Triple(regName, argExpr, Color.RED))
        }

        // render args
        childTarget.asmMiscLabel(relInstr, "(")
        argsToRender.forEachIndexed { argIdx, (argName, argExpr, color) ->
            if (argIdx > 0 && argIdx <= argsToRender.lastIndex) {
                childTarget.asmMiscLabel(relInstr, ", ")
            }
            childTarget.asmMiscLabel(relInstr, argName, color)
            childTarget.asmMiscLabel(relInstr, " = ")
            renderExpr(childTarget, relInstr, argExpr)
        }
        childTarget.asmMiscLabel(relInstr, ")")
        target.addActor(childTarget)
    }

    private fun HorizontalGroup.asmMiscLabel(relInstr: ShlInstr, text: String, color: Color = Color.WHITE): CodeLabel {
        val labelColor = if (relInstr.deadCode) Color.GRAY.cpy() else color
        val label = CodeLabel(relInstr, null, text, false, labelColor, labelStyle)
        addActor(label)
        return label
    }

    private fun HorizontalGroup.asmLabel(relInstr: ShlInstr, text: String, color: Color = Color.WHITE): CodeLabel {
        val labelColor = if (relInstr.deadCode) Color.GRAY.cpy() else color
        val label = CodeLabel(relInstr, null, text, true, labelColor, labelStyle)
        addActor(label)
        return label
    }

    private fun HorizontalGroup.asmExprLabel(relInstr: ShlInstr, expr: ShlExpr, color: Color): CodeLabel {
        return asmExprLabel(relInstr, expr, expr.toExprString(), color)
    }

    private fun HorizontalGroup.asmExprLabel(relInstr: ShlInstr, expr: ShlExpr, exprText: String, color: Color): CodeLabel {
        val labelColor = if (relInstr.deadCode) Color.GRAY.cpy() else color
        val label = CodeLabel(relInstr, expr, exprText, true, labelColor, labelStyle)
        addActor(label)
        return label
    }

    fun highlightVar(varName: String) {
        highlighted.forEach {
            it.restoreOrigColor()
        }
        highlighted.clear()
        table.children.filter { it is HorizontalGroup }.forEach {
            highlightVar(it as HorizontalGroup, varName)
        }
    }

    private fun highlightVar(target: HorizontalGroup, varName: String) {
        target.children.forEach {
            if (it is HorizontalGroup) highlightVar(it, varName)
            if (it is CodeLabel && it.text.toString() == varName) {
                it.highlight()
                highlighted.add(it)
            }
        }
    }

    fun showBreakpoint(addr: Int) {
        dbgLabels[addr]?.let { it.isVisible = true }
    }

    fun hideBreakpoint() {
        dbgLabels.forEach { it.value.isVisible = false }
    }

    inner class CodeLabel(val relInstr: ShlInstr, val shlExpr: ShlExpr?, text: CharSequence, val canHighlight: Boolean,
                          val origColor: Color, style: LabelStyle) : VisLabel(text, style) {
        init {
            color = origColor.cpy()
            touchable = Touchable.enabled
            addListener(object : InputListener() {
                val dumpCtxs = false
                override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                    if (dumpCtxs) {
                        relInstr.dataFlowCtx.dump()
                        relInstr.reachFlowCtx.dump()
                        relInstr.nextLiveFlowCtx.dump()
                    }
                    if (canHighlight) {
                        listener.onHighlight(getText().toString())
                        return true
                    }
                    return false //to allow node dragging
                }
            })
            addListener(object : InputListener() {
                override fun touchDown(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                    if (event.button == Input.Buttons.RIGHT) {
                        appStage.actors.filter { it is PopupMenu }.forEach { it.remove() }
                    }
                    return true
                }

                override fun touchUp(event: InputEvent, x: Float, y: Float, pointer: Int, button: Int) {
                    if (event.button == Input.Buttons.RIGHT) {
                        val pos = Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
                        appStage.screenToStageCoordinates(pos)
                        createContextMenu().showMenu(appStage, pos.x, pos.y)
                    }
                }
            })
        }

        private fun createContextMenu() = popupMenu {
            createCopyAddrMenuItems()
            condSubMenu(text = "To struct", entries = types.getStructs(), entryTransform = { it.name }, wantSeparator = true,
                    condition = { relInstr is ShlMemStoreInstr || (relInstr is ShlAssignInstr && relInstr.src is ShlMemLoad) },
                    change = { struct ->
                        exprMutator.mutateToStructAccess(relInstr, struct)
                        this@GraphNode.listener.init()
                    })

            condMenuItem(text = "To memory access", wantSeparator = true,
                    condition = { relInstr is ShlStructStoreInstr || (relInstr is ShlAssignInstr && relInstr.src is ShlStructLoad) },
                    change = {
                        exprMutator.mutateToMemoryAccess(relInstr)
                        this@GraphNode.listener.init()
                    })
            if (shlExpr != null) {
                createShlExprMenuItems()
                createShlVarMenuItems()
            }
            if (relInstr is ShlBranchInstr) {
                createBranchInstrMenuItems()
            }
            createCommonMenuItems()
        }

        private fun PopupMenu.createCopyAddrMenuItems() {
            menuItem("Copy address") {
                onChange {
                    Gdx.app.clipboard.contents = relInstr.addr.toHex()
                }
            }
            menuItem("Copy physical address") {
                onChange {
                    Gdx.app.clipboard.contents = (0x8804000 + relInstr.addr).toHex()
                }
            }
            addSeparator()
        }

        private fun PopupMenu.createShlExprMenuItems() {
            if (shlExpr !is ShlExpr.ShlSubstitutable) return
            if (!(shlExpr is ShlConst || shlExpr is ShlString || shlExpr is ShlGlobalRef || shlExpr is ShlFuncPointer)) return
            val originValue = when (shlExpr) {
                is ShlConst -> shlExpr.value
                is ShlString -> shlExpr.addr
                is ShlGlobalRef -> shlExpr.addr
                is ShlFuncPointer -> shlExpr.addr
                else -> log.panic(tag, "unsupported expression type for conversion")
            }
            menuItem("Convert") {
                subMenu {
                    if (shlExpr !is ShlConst) {
                        menuItem("To const ${originValue.toHex()}") {
                            onChange {
                                relInstr.substituteReadExpr(shlExpr, true, ShlConst(originValue))
                                this@GraphNode.listener.init()
                            }
                        }
                    }
                    val funcDef = projectIO.getFuncDefByOffset(originValue)
                    if (shlExpr !is ShlFuncPointer && funcDef != null) {
                        menuItem("To func. pointer \"${funcDef.name}\"") {
                            onChange {
                                relInstr.substituteReadExpr(shlExpr, true, ShlFuncPointer(originValue))
                                this@GraphNode.listener.init()
                            }
                        }
                    }
                    val global = globals.getGlobalByAddress(originValue)
                    if (shlExpr !is ShlGlobalRef && global != null) {
                        menuItem("To global \"${global.name}\"") {
                            onChange {
                                relInstr.substituteReadExpr(shlExpr, true, ShlGlobalRef(originValue))
                                this@GraphNode.listener.init()
                            }
                        }
                    }
                    if (shlExpr !is ShlString) {
                        var exprString = ""
                        try {
                            exprString = projectIO.getElfLoader().readString(originValue)
                        } catch (e: Exception) {
                        }
                        if (exprString.isNotBlank()) {
                            menuItem("To string \"$exprString\"") {
                                onChange {
                                    relInstr.substituteReadExpr(shlExpr, true, ShlString(originValue, exprString))
                                    this@GraphNode.listener.init()
                                }
                            }
                        }
                    }
                }
            }
            addSeparator()
        }

        private fun PopupMenu.createShlVarMenuItems() {
            if (shlExpr !is ShlVar) return
            condMenuItem(text = "Rename assigned variable",
                    condition = {
                        (relInstr is ShlAssignInstr && relInstr.dest.compareExpr(shlExpr)) ||
                                (relInstr is ShlCallInstr && relInstr.returnReg != null && relInstr.returnReg!!.compareExpr(shlExpr))
                    },
                    change = {
                        Dialogs.showInputDialog(appStage, "Rename", "New name", true, object : InputDialogAdapter() {
                            override fun finished(newName: String) {
                                exprMutator.renameAssignedVariable(relInstr, newName)
                                this@GraphNode.listener.init()
                            }
                        })
                    })
            menuItem("Propagate and optimize") {
                onChange {
                    exprMutator.propagate(relInstr)
                    exprMutator.optimizeGraph()
                    this@GraphNode.listener.init()
                }
            }
            menuItem("Propagate") {
                onChange {
                    exprMutator.propagate(relInstr)
                    this@GraphNode.listener.init()
                }
            }
            addSeparator()
        }

        private fun PopupMenu.createBranchInstrMenuItems() {
            menuItem("Simplify condition") {
                onChange {
                    exprMutator.simplifyCondition(relInstr)
                    this@GraphNode.listener.init()
                }
            }
            menuItem("Flip condition") {
                onChange {
                    exprMutator.flipBranch(relInstr)
                    this@GraphNode.listener.init()
                }
            }
            menuItem("Eliminate branch") {
                onChange {
                    exprMutator.eliminateBranch(relInstr)
                    this@GraphNode.listener.init()
                }
            }
            addSeparator()
        }

        private fun PopupMenu.createCommonMenuItems() {
            menuItem("Toggle dead code") {
                onChange {
                    relInstr.deadCode = !relInstr.deadCode
                    this@GraphNode.listener.init()
                }
            }
            menuItem("Edit comment") {
                onChange {
                    val dialog = TextAreaInputDialog("Enter comment", object : InputDialogAdapter() {
                        override fun finished(input: String) {
                            relInstr.comment = input
                            this@GraphNode.listener.init()
                        }
                    })
                    dialog.setText(relInstr.comment, selectText = true)
                    appStage.addActor(dialog.fadeIn())
                }
            }
            menuItem("Evaluate") {
                onChange {
                    exprMutator.evaluate(relInstr)
                    this@GraphNode.listener.init()
                }
            }
            addSeparator()
            menuItem("Optimize graph") {
                onChange {
                    exprMutator.optimizeGraph()
                    this@GraphNode.listener.init()
                }
            }
        }

        private fun PopupMenu.condMenuItem(condition: () -> Boolean, text: String, change: () -> Unit,
                                           wantSeparator: Boolean = false): Boolean {
            if (condition() == false) return false
            menuItem(text) {
                onChange {
                    change()
                }
            }
            if (wantSeparator) addSeparator()
            return true
        }

        private fun <T> PopupMenu.condSubMenu(condition: () -> Boolean, text: String, entries: List<T>, entryTransform: (T) -> String,
                                              change: (T) -> Unit, wantSeparator: Boolean = false): Boolean {
            if (condition() == false || entries.isEmpty()) return false
            menuItem(text) {
                subMenu {
                    entries.forEach { entry ->
                        menuItem(entryTransform(entry)) {
                            onChange {
                                change(entry)
                            }
                        }
                    }
                }
            }
            if (wantSeparator) addSeparator()
            return true
        }


        fun highlight() {
            color = highlightSyntaxColor
        }

        fun restoreOrigColor() {
            color = origColor
        }
    }

    interface Listener {
        fun onHighlight(expr: String)
        fun init()
    }
}

class CodeNodeEdge(val node: GraphNode, val edgeType: EdgeType, val edgeKind: EdgeKind, var points: List<Vector2>? = null)
