package dev.nowtech.builtin.devtools

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefBrowser
import javax.swing.JComponent

@Service(Service.Level.PROJECT)
class DevToolsProjectService(private val project: Project) : Disposable {

  private var browser: JBCefBrowser? = null

  private fun getOrCreateBrowser(): JBCefBrowser {
    val existing = browser
    if (existing != null && !existing.isDisposed) {
      return existing
    }
    return JBCefBrowser().also { created ->
      browser = created
      devToolsLog.info("Built-in DevTools: created tool window JCEF browser for project ${project.name}")
    }
  }

  fun browserComponent(): JComponent = getOrCreateBrowser().component

  fun loadInspector(url: String) {
    getOrCreateBrowser().loadURL(url)
  }

  override fun dispose() {
    browser?.dispose()
    browser = null
  }

  companion object {
    fun getInstance(project: Project): DevToolsProjectService =
      project.getService(DevToolsProjectService::class.java)
  }
}
