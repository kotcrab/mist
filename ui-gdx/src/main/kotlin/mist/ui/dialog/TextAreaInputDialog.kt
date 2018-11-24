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

import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.util.TableUtils
import com.kotcrab.vis.ui.util.dialog.InputDialogListener
import com.kotcrab.vis.ui.widget.ButtonBar
import com.kotcrab.vis.ui.widget.ButtonBar.ButtonType
import com.kotcrab.vis.ui.widget.ScrollableTextArea
import com.kotcrab.vis.ui.widget.VisTextButton
import com.kotcrab.vis.ui.widget.VisWindow
import ktx.actors.onChange

/** @author Kotcrab */

class TextAreaInputDialog(title: String, private val listener: InputDialogListener) : VisWindow(title) {
    private val field: ScrollableTextArea
    private val okButton: VisTextButton
    private val cancelButton: VisTextButton

    init {
        TableUtils.setSpacingDefaults(this)
        isModal = true
        addCloseButton()
        closeOnEscape()
        field = ScrollableTextArea("")
        cancelButton = VisTextButton(ButtonType.CANCEL.text)
        okButton = VisTextButton(ButtonType.OK.text)

        val buttonBar = ButtonBar()
        buttonBar.isIgnoreSpacing = true
        buttonBar.setButton(ButtonType.CANCEL, cancelButton)
        buttonBar.setButton(ButtonType.OK, okButton)

        add(field.createCompatibleScrollPane()).padTop(3f).spaceBottom(4f).grow()
        row()
        add(buttonBar.createTable()).padBottom(3f).right()
        addListeners()
        setSize(500f, 300f)
        centerWindow()
    }

    override fun close() {
        super.close()
        listener.canceled()
    }

    override fun setStage(stage: Stage?) {
        super.setStage(stage)
        if (stage != null) {
            field.focusField()
        }
    }

    fun setText(text: String, selectText: Boolean = false) {
        field.text = text
        field.cursorPosition = text.length
        if (selectText) {
            field.selectAll()
        }
    }

    private fun addListeners() {
        okButton.onChange {
            listener.finished(field.text)
            fadeOut()
        }
        cancelButton.onChange {
            close()
        }
    }
}
