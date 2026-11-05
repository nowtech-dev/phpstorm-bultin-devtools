package dev.nowtech.builtin.devtools

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefBrowserBase
import org.cef.browser.CefBrowser
import org.cef.handler.CefContextMenuHandlerAdapter
import org.cef.handler.CefKeyboardHandlerAdapter
import org.cef.misc.BoolRef
import java.awt.Component
import java.awt.Container
import javax.swing.JComponent

object DevToolsJcefHook {

  private const val HOOK_MARKER = "dev.nowtech.builtin.devtools.hookInstalled"
  private const val HANDLERS_PROPERTY = "dev.nowtech.builtin.devtools.handlersAttached"

  private const val JB_DEVTOOLS_COMMAND_ID = 28500
  private const val MENU_COMMAND_ID = 28650
  private const val MENU_LABEL = "Open DevTools"
  private const val VK_F12 = 123

  fun installForProject(project: Project) {
    ApplicationManager.getApplication().invokeLater({
      val ideFrame = com.intellij.openapi.wm.WindowManager.getInstance().getFrame(project) ?: return@invokeLater
      val root = ideFrame.rootPane ?: return@invokeLater
      installOnRoot(root, project)
    }, ModalityState.any())
  }

  fun installOnRoot(root: JComponent, project: Project?) {
    if (root.getClientProperty(HOOK_MARKER) != true) {
      root.putClientProperty(HOOK_MARKER, true)
      root.addHierarchyListener { scanAndHook(root, project) }
    }
    scanAndHook(root, project)
  }

  fun scanAndHook(root: Component, project: Project?) {
    walkComponents(root) { component ->
      browserFromComponent(component)?.let { ensureHooked(it, project) }
    }
    JBCefBrowserBase.getFocusedBrowser()?.let { ensureHooked(it, project) }
  }

  private fun walkComponents(root: Component, visit: (Component) -> Unit) {
    visit(root)
    if (root is Container) {
      for (child in root.components) {
        walkComponents(child, visit)
      }
    }
  }

  private fun browserFromComponent(component: Component): JBCefBrowserBase? {
    var current: Component? = component
    while (current != null) {
      if (current is JComponent) {
        val attached = current.getClientProperty(JBCefBrowserBase.JBCEFBROWSER_INSTANCE_PROP)
        if (attached is JBCefBrowserBase) {
          return attached
        }
      }
      current = current.parent
    }
    return null
  }

  fun ensureHooked(browser: JBCefBrowserBase, project: Project?) {
    if (browser.isDisposed) {
      return
    }
    if (browser.getProperty(HANDLERS_PROPERTY) == true) {
      return
    }
    scheduleAttach(browser, project, 0)
  }

  private fun scheduleAttach(browser: JBCefBrowserBase, project: Project?, attempt: Int) {
    if (browser.isDisposed) {
      browser.setProperty(HANDLERS_PROPERTY, null)
      return
    }
    if (browser.isCefBrowserCreated) {
      attachHandlers(browser, project)
      devToolsLog.info("Built-in DevTools: hooked JCEF browser (Open DevTools + F12)")
      return
    }
    if (attempt >= 120) {
      devToolsLog.warn("Built-in DevTools: gave up hooking JCEF browser (CEF not created)")
      return
    }
    ApplicationManager.getApplication().invokeLater(
      { scheduleAttach(browser, project, attempt + 1) },
      ModalityState.any(),
    )
  }

  private fun attachHandlers(browser: JBCefBrowserBase, project: Project?) {
    val cefBrowser = browser.cefBrowser ?: return
    val client = browser.getJBCefClient()
    client.addContextMenuHandler(DevToolsContextMenuHandler(browser, project), cefBrowser)
    client.addKeyboardHandler(DevToolsKeyboardHandler(browser, project), cefBrowser)
    browser.setProperty(HANDLERS_PROPERTY, true)
  }

  private class DevToolsContextMenuHandler(
    private val browser: JBCefBrowserBase,
    private val project: Project?,
  ) : CefContextMenuHandlerAdapter() {

    override fun onBeforeContextMenu(
      browser: CefBrowser,
      frame: org.cef.browser.CefFrame,
      params: org.cef.callback.CefContextMenuParams,
      model: org.cef.callback.CefMenuModel,
    ) {
      if (model.getIndexOf(MENU_COMMAND_ID) >= 0) {
        return
      }
      if (!model.insertItemAt(0, MENU_COMMAND_ID, MENU_LABEL)) {
        model.addItem(MENU_COMMAND_ID, MENU_LABEL)
      }
    }

    override fun onContextMenuCommand(
      browser: CefBrowser,
      frame: org.cef.browser.CefFrame,
      params: org.cef.callback.CefContextMenuParams,
      commandId: Int,
      eventFlags: Int,
    ): Boolean {
      if (commandId != MENU_COMMAND_ID && commandId != JB_DEVTOOLS_COMMAND_ID) {
        return false
      }
      DevToolsCdpOpener.open(project, this.browser)
      return true
    }
  }

  private class DevToolsKeyboardHandler(
    private val browser: JBCefBrowserBase,
    private val project: Project?,
  ) : CefKeyboardHandlerAdapter() {

    override fun onPreKeyEvent(
      browser: CefBrowser,
      event: org.cef.handler.CefKeyboardHandler.CefKeyEvent,
      isKeyboardShortcut: BoolRef,
    ): Boolean {
      if (event.windows_key_code != VK_F12) {
        return false
      }
      DevToolsCdpOpener.open(project, this.browser)
      return true
    }
  }
}
