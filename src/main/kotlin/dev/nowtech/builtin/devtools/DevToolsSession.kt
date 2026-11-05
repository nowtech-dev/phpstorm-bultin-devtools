package dev.nowtech.builtin.devtools

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.jcef.JBCefBrowserBase
import dev.nowtech.builtin.devtools.toolwindow.DevToolsToolWindowFactory
import java.lang.ref.WeakReference

@Service(Service.Level.PROJECT)
class DevToolsSession(private val project: Project) {

  private var lastBrowserRef: WeakReference<JBCefBrowserBase>? = null
  private var lastPageUrl: String = ""
  private var reconnectPending = false

  fun trackOpen(browser: JBCefBrowserBase) {
    if (DevToolsProjectService.isInspectorBrowser(browser)) {
      return
    }
    lastBrowserRef = WeakReference(browser)
    lastPageUrl = browser.cefBrowser?.url.orEmpty()
  }

  fun onPreviewBrowserChanged(browser: JBCefBrowserBase, reason: String) {
    if (DevToolsProjectService.isInspectorBrowser(browser)) {
      return
    }
    val url = browser.cefBrowser?.url.orEmpty()
    val mustReconnect = reason == "cef recreated" || url != lastPageUrl
    lastBrowserRef = WeakReference(browser)
    lastPageUrl = url
    if (mustReconnect) {
      scheduleReconnect(reason)
    }
  }

  fun scheduleReconnect(reason: String) {
    if (!isDevToolsToolWindowVisible()) {
      return
    }
    if (reconnectPending) {
      return
    }
    reconnectPending = true
    ApplicationManager.getApplication().invokeLater({
      reconnectPending = false
      reconnect(reason)
    }, ModalityState.any())
  }

  fun reconnect(reason: String) {
    if (!isDevToolsToolWindowVisible()) {
      return
    }
    val browser = resolveBrowser() ?: return
    devToolsLog.info("Built-in DevTools: reconnect ($reason) for ${browser.cefBrowser?.url}")
    DevToolsCdpOpener.open(project, browser)
  }

  private fun resolveBrowser(): JBCefBrowserBase? {
    val last = lastBrowserRef?.get()?.takeIf { !it.isDisposed && !DevToolsProjectService.isInspectorBrowser(it) }
    if (last != null) {
      return last
    }
    return JBCefBrowserBase.getFocusedBrowser()?.takeUnless { DevToolsProjectService.isInspectorBrowser(it) }
      ?: DevToolsBrowserLocator.findActiveBrowser(project)
        ?.takeUnless { DevToolsProjectService.isInspectorBrowser(it) }
  }

  private fun isDevToolsToolWindowVisible(): Boolean {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(DevToolsToolWindowFactory.TOOL_WINDOW_ID)
    return toolWindow != null && toolWindow.isVisible
  }

  companion object {
    fun getInstance(project: Project): DevToolsSession = project.getService(DevToolsSession::class.java)
  }
}
