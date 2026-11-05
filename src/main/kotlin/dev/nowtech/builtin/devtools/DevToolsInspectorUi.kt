package dev.nowtech.builtin.devtools

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import dev.nowtech.builtin.devtools.settings.DevToolsSettings
import dev.nowtech.builtin.devtools.settings.InspectorHost
import dev.nowtech.builtin.devtools.toolwindow.DevToolsToolWindowFactory

object DevToolsInspectorUi {

  fun open(project: Project?, inspectorUrl: String) {
    when (DevToolsSettings.getInstance().resolveInspectorHost()) {
      InspectorHost.EXTERNAL_BROWSER -> openExternal(project, inspectorUrl)
      InspectorHost.IDE_TOOL_WINDOW -> openToolWindow(project, inspectorUrl)
    }
  }

  private fun openExternal(project: Project?, inspectorUrl: String) {
    DevToolsExternalBrowser.open(inspectorUrl)
    notify(project, "DevTools opened in your default browser.", inspectorUrl)
  }

  private fun openToolWindow(project: Project?, inspectorUrl: String) {
    val targetProject =
      project
        ?: com.intellij.openapi.project.ProjectManager.getInstance().openProjects.firstOrNull()
    if (targetProject == null) {
      openExternal(null, inspectorUrl)
      return
    }
    val toolWindow = ToolWindowManager.getInstance(targetProject).getToolWindow(DevToolsToolWindowFactory.TOOL_WINDOW_ID)
    if (toolWindow == null) {
      openExternal(targetProject, inspectorUrl)
      return
    }
    DevToolsProjectService.getInstance(targetProject).loadInspector(inspectorUrl)
    toolWindow.activate(null)
    notify(targetProject, "DevTools opened in the IDE tool window (bottom).", inspectorUrl)
  }

  private fun notify(project: Project?, message: String, inspectorUrl: String) {
    Notifications.Bus.notify(
      Notification(
        "Built-in Browser DevTools",
        "DevTools opened",
        message,
        NotificationType.INFORMATION,
      ),
    )
    devToolsLog.info("Built-in DevTools: $message url=$inspectorUrl")
  }
}
