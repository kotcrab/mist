package mist.ui.dialog

import com.kotcrab.vis.ui.widget.VisWindow
import com.kotcrab.vis.ui.widget.tabbedpane.Tab
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane
import ktx.vis.table
import mist.ui.util.StaticMutableListAdapter
import mist.ui.util.onChange
import mist.ui.util.ui
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class UnsavedTabsDialog(private val tabbedPane: TabbedPane, private val cont: Continuation<Result>) :
  VisWindow("Unsaved resources") {
  private val adapter = TabAdapter()

  init {
    isModal = true
    isResizable = true
    addCloseButton()
    closeOnEscape()
    ui {
      defaults().left()
      listView(adapter) { listCell ->
        listCell.grow()
      }
      table(true) { buttonsCell ->
        buttonsCell.growY()
        top()
        defaults().growX()
        textButton("Save") { onChange(::onSave) }
        row()
        textButton("Save All") { onChange(::onSaveAll) }
        row()
        textButton("Discard all") { onChange(::onDiscardAll) }
        row()
        textButton("Cancel") { onChange(::onCancel) }
      }
    }
    adapter.itemsChanged()
    setSize(530f, 320f)
    centerWindow()
  }

  private fun onSave() {
    adapter.selection.forEach { it.save() }
    adapter.itemsChanged()
  }

  private fun onSaveAll() {
    tabbedPane.tabs.filter { it.isDirty }.forEach { it.save() }
    remove()
    cont.resume(Result.Finished)
  }

  private fun onDiscardAll() {
    tabbedPane.tabs.filter { it.isDirty }.forEach { tabbedPane.remove(it, true) }
    remove()
    cont.resume(Result.Finished)
  }

  private fun onCancel() {
    fadeOut()
    cont.resume(Result.Canceled)
  }

  override fun close() {
    super.close()
    cont.resume(Result.Canceled)
  }

  private inner class TabAdapter(val filteredList: MutableList<Tab> = mutableListOf()) :
    StaticMutableListAdapter<Tab>(filteredList) {
    init {
      selectionMode = SelectionMode.SINGLE
    }

    override fun itemsChanged() {
      filteredList.clear()
      filteredList.addAll(tabbedPane.tabs.filter { it.isDirty })
      super.itemsChanged()
    }

    override fun createView(tab: Tab) = table {
      left()
      label(tab.tabTitle)
    }
  }

  enum class Result {
    Canceled, Finished
  }
}
