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

package mist.ui.dialog

import com.kotcrab.vis.ui.util.dialog.Dialogs
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.VisWindow
import ktx.actors.onChange
import ktx.vis.table
import mist.io.LocalHistory
import mist.io.LocalHistoryEntry
import mist.ui.util.StaticMutableListAdapter
import mist.ui.util.WindowResultListener
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


/** @author Kotcrab */

class LocalHistoryDialog(
    private val entryReader: LocalHistory.EntryReader,
    private val listener: WindowResultListener<LocalHistoryEntry>
) : VisWindow("Local History") {
    init {
        isModal = true
        isResizable = true
        addCloseButton()
        closeOnEscape()

        add(table(true) {
            defaults().left()
            val adapter = EntryAdapter(entryReader.asSequence().toMutableList())
            listView(adapter) { listViewCell ->
                listViewCell.grow()
            }
            table(true) { buttonsCell ->
                buttonsCell.growY()
                top()
                defaults().growX()
                textButton("Revert") { _ ->
                    onChange {
                        val selection = adapter.selection
                        if (selection.size > 0) {
                            fadeOut()
                            listener.finished(selection.first())
                        } else {
                            Dialogs.showOKDialog(stage, "Message", "Select revision before reverting")
                        }
                    }
                }
                row()
                textButton("Cancel") {
                    onChange {
                        fadeOut()
                        listener.canceled()
                    }
                }
            }
            adapter.itemsChanged()
        }).grow()
        setSize(440f, 600f)
        centerWindow()
    }

    override fun close() {
        super.close()
        listener.canceled()
    }

    private inner class EntryAdapter(list: MutableList<LocalHistoryEntry>) :
        StaticMutableListAdapter<LocalHistoryEntry>(list) {
        init {
            selectionMode = SelectionMode.SINGLE
            itemsSorter = Comparator { o1, o2 -> o2.epochSecond.compareTo(o1.epochSecond) }
        }

        override fun createView(entry: LocalHistoryEntry): VisTable {
            return table {
                left()
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val localDate =
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(entry.epochSecond), Clock.systemDefaultZone().zone)
                label(localDate.format(formatter))
            }
        }
    }
}
