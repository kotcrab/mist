package mist

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.kotcrab.vis.ui.VisUI
import com.kotcrab.vis.ui.util.dialog.Dialogs
import com.kotcrab.vis.ui.util.value.PrefHeightIfVisibleValue
import com.kotcrab.vis.ui.widget.MenuBar
import com.kotcrab.vis.ui.widget.PopupMenu
import com.kotcrab.vis.ui.widget.VisSplitPane
import com.kotcrab.vis.ui.widget.VisTable
import com.kotcrab.vis.ui.widget.file.FileChooser
import com.kotcrab.vis.ui.widget.tabbedpane.Tab
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPane
import com.kotcrab.vis.ui.widget.tabbedpane.TabbedPaneAdapter
import kotlinx.coroutines.async
import ktx.actors.onChange
import ktx.async.KtxAsync
import ktx.inject.Context
import ktx.vis.menu
import ktx.vis.menuBar
import ktx.vis.menuItem
import mist.io.AsmFunctionIO
import mist.io.ProjectIO
import mist.shl.ShlDefsChangeType
import mist.shl.ShlDefsChanged
import mist.shl.ShlFunctionDef
import mist.ui.Assets
import mist.ui.dialog.*
import mist.ui.node.FlowTab
import mist.ui.node.GraphTab
import mist.ui.node.TabProvidesMenu
import mist.ui.util.MixedRenderingTab
import mist.ui.util.RemoteDebugger
import mist.ui.util.startForResult
import mist.util.MistLogger
import mist.util.logTag
import java.io.File

fun main(args: Array<String>) {
  if (args.isEmpty() || File(args[0]).exists() == false) error("specify project directory as first argument")
  val app = App(File(args[0]))
  val c = Lwjgl3ApplicationConfiguration()
  c.setTitle("mist")
  c.setWindowedMode(1280, 720)
  c.setWindowListener(object : Lwjgl3WindowAdapter() {
    override fun closeRequested(): Boolean {
      KtxAsync.async { app.requestExit() }
      return false
    }
  })
  Lwjgl3Application(app, c)
}

class App(val projectDir: File) : ApplicationListener {
  private val tag = logTag()
  private val context = Context()

  private val log = MistLogger()
  private val projectIO = ProjectIO(projectDir, log)
  private val remoteDebugger = RemoteDebugger(log)

  private lateinit var assets: Assets
  private lateinit var appStage: Stage
  private lateinit var root: VisTable
  private lateinit var tabbedPane: TabbedPane
  private lateinit var tabMenu: PopupMenu
  private lateinit var funcsPanel: FuncsPanel
  private val openGraphTabs = mutableMapOf<Int, GraphTab>()

  override fun create() {
    VisUI.load()
    FileChooser.setDefaultPrefsName("com.kotcrab.mist.filechooserprefs")
    assets = Assets()
    appStage = Stage(ScreenViewport())
    root = VisTable()
    tabbedPane = TabbedPane()
    context.register {
      bindSingleton(projectIO)
      bindSingleton(log)
      bindSingleton(appStage)
      bindSingleton(assets)
      bindSingleton(remoteDebugger)
    }
    createFuncsPanel()
    buildRoot(createMenuBar(), tabbedPane)
    createInitialTabs()
  }

  private fun createInitialTabs() {
    val flowTab = FlowTab(
      context,
      { disassembleFunc(it) },
      { funcsPanel.showEditFuncWindow(projectIO.getFuncDefByName(it)!!) })
    tabbedPane.add(flowTab)
    tabbedPane.add(TypesTab(context, { notifyTabsShlDefsChanged(ShlDefsChangeType.Types) }))
    val startingFunc = "start"
    projectIO.getFuncDefByName(startingFunc)?.let { flowTab.centerAround(it) }
    // disassembleFunc(startingFunc)
  }

  private fun createMenuBar() = menuBar {
    menu("File") {
      menuItem("Save project") {
        onChange {
          projectIO.saveProject()
        }
      }
      addSeparator()
      menuItem("Save current") {
        onChange {
          val tab = tabbedPane.activeTab
          if (tab.isSavable) tab.save()
        }
      }
      addSeparator()
      menuItem("Dump names") {
        onChange {
          projectIO.dumpNames()
        }
      }
    }
    tabMenu = menu("Tab")
    menu("Debugger") {
      menuItem("Connect") {
        onChange {
          remoteDebugger.connect()
        }
      }
      menuItem("Disconnect") {
        onChange {
          remoteDebugger.disconnect()
        }
      }
      menuItem("Open memory viewer") {
        onChange {
          appStage.addActor(MemoryViewerWindow(context, remoteDebugger).fadeIn())
        }
      }
    }
  }

  private fun createFuncsPanel() {
    funcsPanel = FuncsPanel(context, object : FuncsWindowListener {
      override fun onFuncEditWindowClose() {
        projectIO.saveProject()
        notifyTabsShlDefsChanged(ShlDefsChangeType.FunctionDefs)
      }

      override fun onFuncDoubleClick(def: ShlFunctionDef) {
        val tab = tabbedPane.activeTab
        if (tab is FlowTab) {
          tab.centerAround(def)
        } else {
          disassembleFunc(def.name)
        }
      }
    })
  }

  private fun notifyTabsShlDefsChanged(type: ShlDefsChangeType) {
    tabbedPane.tabs.forEach { tab ->
      if (tab is ShlDefsChanged) {
        tab.shlDefsChanged(type)
      }
    }
  }

  private fun disassembleFunc(funcName: String) {
    try {
      val def = projectIO.getFuncDefByName(funcName)
        ?: log.panic(tag, "no such function def for name $funcName")
      val existingTab = openGraphTabs[def.offset]
      if (existingTab != null) {
        tabbedPane.switchTab(existingTab)
      } else {
        val asmFunctionIO = AsmFunctionIO(projectIO, def, MistLogger())
        val newTab = GraphTab(context, asmFunctionIO, def, { disassembleFunc(it.name) })
        openGraphTabs[def.offset] = newTab
        tabbedPane.add(newTab)
      }
    } catch (e: Exception) {
      e.printStackTrace()
      Dialogs.showErrorDialog(appStage, "Failed to decompile function", e)
    }
  }

  private fun buildRoot(menuBar: MenuBar, tabbedPane: TabbedPane) {
    val tabContent = VisTable()
    tabContent.touchable = Touchable.enabled
    val splitPane = VisSplitPane(funcsPanel, tabContent, false)
    splitPane.setSplitAmount(0.20f)
    tabbedPane.addListener(object : TabbedPaneAdapter() {
      override fun switchedTab(tab: Tab) {
        tabContent.clear()
        tabContent.add(tab.contentTable).grow()
        tabMenu.clear()
        if (tab is TabProvidesMenu) {
          tab.fillMenu(tabMenu)
        } else {
          tabMenu.menuItem("No extra options") {
            isDisabled = true
          }
        }
      }

      override fun removedTab(tab: Tab) {
        if (tab is GraphTab) {
          openGraphTabs.remove(tab.def.offset)
        }
      }

      override fun removedAllTabs() {
        openGraphTabs.clear()
      }
    })
    root.setFillParent(true)
    root.top()
    root.add(menuBar.table).growX().row()
    root.add(tabbedPane.table).growX().height(PrefHeightIfVisibleValue.INSTANCE).row()
    root.add(splitPane).grow().row()
    appStage.addActor(root)
    Gdx.input.inputProcessor = appStage
  }

  override fun render() {
    val activeTab = tabbedPane.activeTab
    if (activeTab is MixedRenderingTab) {
      activeTab.update()
    }
    appStage.act()
    Gdx.gl.glClearColor(0.15f, 0.15f, 0.15f, 1f)
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
    Gdx.gl.glEnable(GL20.GL_BLEND)
    if (activeTab is MixedRenderingTab) {
      activeTab.render()
    }
    appStage.draw()
  }

  override fun dispose() {
    projectIO.saveProject()
    assets.dispose()
  }

  override fun resize(width: Int, height: Int) {
    tabbedPane.tabs.forEach { tab ->
      if (tab is MixedRenderingTab) {
        tab.resize(width, height)
      }
    }
    if (width == 0 && height == 0) return
    appStage.viewport.update(width, height, true)
  }

  suspend fun requestExit() {
    if (appStage.actors.any { it is UnsavedTabsDialog }) return
    if (tabbedPane.tabs.all { it.isDirty == false }) {
      Gdx.app.exit()
    }
    val result = appStage.startForResult(::UnsavedTabsDialog, tabbedPane)
    if (result == UnsavedTabsDialog.Result.Canceled) return
    requestExit()
  }

  override fun resume() {
  }

  override fun pause() {
  }
}
