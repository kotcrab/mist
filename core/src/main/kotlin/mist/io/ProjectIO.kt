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

import kio.util.child
import kio.util.readJson
import kio.util.toHex
import kio.util.writeJson
import mist.shl.ShlFunctionDef
import mist.shl.ShlGlobals
import mist.shl.ShlProject
import mist.shl.ShlTypes
import mist.ui.node.ExternalLayout
import mist.ui.node.FlowApiSet
import mist.ui.node.FlowNodeDto
import mist.util.DecompLog
import mist.util.logTag
import java.io.File

/** @author Kotcrab */

class ProjectIO(private val projectDir: File, private val log: DecompLog) {
    private val tag = logTag()

    private val tmpDir = projectDir.child("tmp")
    private val funcsDir = projectDir.child("funcs")
    private val toolsDir = projectDir.child("tools")

    private val ebootBin = projectDir.child("EBOOT.BIN")
    private val projectJson = projectDir.child("project.json")
    private val typesJson = projectDir.child("types.json")
    private val globalsJson = projectDir.child("globals.json")
    private val userPrefsJson = projectDir.child("userPrefs.json")

    private val flowGraph = projectDir.child("flowGraph.json")
    private val flowApis = projectDir.child("flowApis.json")

    private val elfLoader = PspElfLoader(ElfFile(ebootBin), log)
    private var shlProject: ShlProject
    private var shlTypes: ShlTypes
    private var shlGlobals: ShlGlobals
    private var userPrefs: ProjectUserPrefs

    init {
        tmpDir.mkdir()
        funcsDir.mkdir()
        toolsDir.mkdir()
        if (arrayOf(projectDir, tmpDir, funcsDir, toolsDir, ebootBin, flowGraph, flowApis).any { it.exists() == false }) {
            log.panic(tag, "missing required project files")
        }
        shlProject = if (projectJson.exists()) projectJson.readJson() else ShlProject()
        shlTypes = if (typesJson.exists()) typesJson.readJson() else ShlTypes()
        shlGlobals = if (globalsJson.exists()) globalsJson.readJson() else ShlGlobals()
        userPrefs = if (userPrefsJson.exists()) userPrefsJson.readJson() else ProjectUserPrefs()
        log.info(tag, "loaded project")
    }

    fun saveProject() {
        log.info(tag, "save project")
        projectJson.writeJson(shlProject)
        typesJson.writeJson(shlTypes)
        globalsJson.writeJson(shlGlobals)
        userPrefsJson.writeJson(userPrefs)
    }

    fun loadFlowApis(): List<FlowApiSet> {
        return flowApis.readJson()
    }

    fun loadFlowGraph(): List<FlowNodeDto> {
        return flowGraph.readJson()
    }

    fun saveFlowGraph(nodes: List<FlowNodeDto>) {
        flowGraph.writeJson(nodes)
    }

    fun getFuncs(): List<ShlFunctionDef> = shlProject.functionDefs

    fun getFuncDefByName(name: String): ShlFunctionDef? {
        return shlProject.functionDefs.firstOrNull { it.name == name }
    }

    fun getFuncDefByOffset(offset: Int): ShlFunctionDef? {
        return shlProject.functionDefs.firstOrNull { it.offset == offset }
    }

    fun getFuncDir(def: ShlFunctionDef): File {
        val dir = funcsDir.child(def.offset.toString())
        dir.mkdir()
        return dir
    }

    fun getElfLoader() = elfLoader

    fun provideLayout() = ExternalLayout(toolsDir.child("layout.exe"), log)

    fun getTypes(): ShlTypes {
        return shlTypes
    }

    fun getGlobals(): ShlGlobals {
        return shlGlobals
    }

    fun getUserPrefs(): ProjectUserPrefs {
        return userPrefs
    }

    fun dumpNames() {
        log.trace(tag, "dumping names")
        val sb = StringBuilder()
        getFuncs().forEach {
            if (it.name.startsWith("sub_")) return@forEach
            val safeName = it.name.replace("+", "_")
            val line = userPrefs.funcNamesDumpFormat
                    .replace("%addr", it.offset.toHex())
                    .replace("%name", safeName)
            sb.append(line)
            sb.append("\r\n")
        }
        tmpDir.child("funcNames.txt").writeText(sb.toString())
    }
}

class ProjectUserPrefs {
    var lastFuncSearch = ""
    var funcNamesDumpFormat = ""
}
