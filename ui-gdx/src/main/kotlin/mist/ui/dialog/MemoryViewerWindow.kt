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

package mist.ui.dialog

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.kotcrab.vis.ui.widget.VisWindow
import ktx.inject.Context
import mist.ui.Assets
import mist.ui.util.RemoteDebugger
import mist.util.logTag

class MemoryViewerWindow(val context: Context, val debugger: RemoteDebugger) : VisWindow("Memory") {
  private val tag = logTag()
  private val assets: Assets = context.inject()

  private val labelStyle = Label.LabelStyle(assets.consolasSmallFont, Color.WHITE.cpy())

  init {
    isModal = true
    isResizable = true
    addCloseButton()
    closeOnEscape()

//        launch(KtxAsync) {
//            val memory = debugger.getClient()!!.getMemory(0x8804000, 1024).mem
//
//            val table = VisTable(false)
//            val scrollPane = VisScrollPane(table)
//            scrollPane.setFadeScrollBars(false)
//            scrollPane.setFlickScroll(false)
//            table.left().top()
//            table.defaults().left()
//
//            var first = true
//            for (addr in 0x0000 until 1024) {
//                val line = addr % 17 == 0
//                if (line) {
//                    if (first == false) table.row()
//                    val addrLabel = VisLabel(addr.toWHex(), labelStyle)
//                    table.add(addrLabel).padRight(5f)
//                    first = false
//                }
//
//                val label = VisLabel(memory[addr].toWHex(), labelStyle)
//                table.add(label).width(27.0f)
//            }
//
//            add(scrollPane).grow().pad(3f)
//        }
    setSize(800f, 400f)
    centerWindow()
  }
}
