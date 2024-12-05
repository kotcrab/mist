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
