package dev.nowtech.builtin.devtools.toolwindow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import dev.nowtech.builtin.devtools.DevToolsProjectService

class DevToolsToolWindowFactory : ToolWindowFactory, DumbAware {

  override fun shouldBeAvailable(project: Project): Boolean = true

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val service = DevToolsProjectService.getInstance(project)
    val content =
      ContentFactory.getInstance().createContent(service.browserComponent(), "", false).apply {
        isCloseable = false
      }
    toolWindow.contentManager.addContent(content)
  }

  companion object {
    const val TOOL_WINDOW_ID = "Built-in Browser DevTools"
  }
}
