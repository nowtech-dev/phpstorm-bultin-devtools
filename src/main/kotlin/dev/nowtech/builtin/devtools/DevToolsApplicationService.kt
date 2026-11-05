package dev.nowtech.builtin.devtools

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ex.WindowManagerEx
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.util.Alarm
import dev.nowtech.builtin.devtools.settings.DevToolsSettings
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.Window
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.RootPaneContainer

@Service(Service.Level.APP)
class DevToolsApplicationService : Disposable {

  private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
  private var scanTicks = 0

  private val f12Dispatcher =
    KeyEventDispatcher { event ->
      if (event.id != KeyEvent.KEY_PRESSED || event.keyCode != KeyEvent.VK_F12) {
        return@KeyEventDispatcher false
      }
      if (!DevToolsSettings.getInstance().getState().enableContextMenuOnStartup) {
        return@KeyEventDispatcher false
      }
      val browser =
        JBCefBrowserBase.getFocusedBrowser()
          ?: DevToolsBrowserLocator.findActiveBrowser(DevToolsBrowserLocator.projectForFocusedComponent())
      if (browser == null) {
        return@KeyEventDispatcher false
      }
      val project = DevToolsBrowserLocator.projectForFocusedComponent()
      DevToolsJcefHook.ensureHooked(browser, project)
      DevToolsCdpOpener.open(project, browser)
      event.consume()
      true
    }

  init {
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(f12Dispatcher)
    schedulePeriodicScan()
  }

  fun scanAllWindows() {
    ApplicationManager.getApplication().invokeLater {
      for (frame in WindowManagerEx.getInstanceEx().allProjectFrames) {
        val root = frame.component?.rootPane ?: continue
        DevToolsJcefHook.installOnRoot(root, frame.project)
      }
      for (window in Window.getWindows()) {
        if (window is RootPaneContainer) {
          val root = window.rootPane
          if (root != null) {
            DevToolsJcefHook.installOnRoot(root, null)
          }
        }
      }
    }
  }

  private fun schedulePeriodicScan() {
    scanAllWindows()
    if (scanTicks++ < 30) {
      alarm.addRequest({ schedulePeriodicScan() }, 2_000)
    }
  }

  override fun dispose() {
    KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(f12Dispatcher)
    alarm.cancelAllRequests()
  }

  companion object {
    fun getInstance(): DevToolsApplicationService =
      ApplicationManager.getApplication().getService(DevToolsApplicationService::class.java)
  }
}
