package dev.nowtech.builtin.devtools

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import javax.swing.JComponent

@Service(Service.Level.PROJECT)
class DevToolsProjectService(private val project: Project) : Disposable {

  companion object {
    const val INSPECTOR_PANEL_PROP = "dev.nowtech.builtin.devtools.inspectorPanel"

    fun isInspectorBrowser(browser: JBCefBrowserBase): Boolean =
      browser.getProperty(INSPECTOR_PANEL_PROP) == true

    fun getInstance(project: Project): DevToolsProjectService =
      project.getService(DevToolsProjectService::class.java)
  }

  private var browser: JBCefBrowser? = null

  private fun getOrCreateBrowser(): JBCefBrowser {
    val existing = browser
    if (existing != null && !existing.isDisposed) {
      return existing
    }
    return JBCefBrowser().also { created ->
      browser = created
      created.setProperty(INSPECTOR_PANEL_PROP, true)
      DevToolsCompactUi.attachIfNeeded(created)
      devToolsLog.info("Built-in DevTools: created tool window JCEF browser for project ${project.name}")
    }
  }

  fun browserComponent(): JComponent = getOrCreateBrowser().component

  fun loadInspector(url: String) {
    val jb = getOrCreateBrowser()
    jb.loadURL(url)
    ApplicationManager.getApplication().invokeLater {
      DevToolsCompactUi.reapplyTheme(jb)
    }
  }

  override fun dispose() {
    browser?.dispose()
    browser = null
  }

}
