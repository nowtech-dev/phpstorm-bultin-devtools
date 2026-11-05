package dev.nowtech.builtin.devtools

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import dev.nowtech.builtin.devtools.toolwindow.DevToolsToolWindowFactory

@Service(Service.Level.PROJECT)
class DevToolsProjectWatcher(project: Project) : Disposable {

  private val connection = project.messageBus.connect(this)

  init {
    connection.subscribe(
      ToolWindowManagerListener.TOPIC,
      object : ToolWindowManagerListener {
        override fun toolWindowShown(id: String, toolWindow: com.intellij.openapi.wm.ToolWindow) {
          if (id == DevToolsToolWindowFactory.TOOL_WINDOW_ID) {
            DevToolsSession.getInstance(project).scheduleReconnect("tool window shown")
          }
        }
      },
    )
  }

  override fun dispose() {
    connection.dispose()
  }

  companion object {
    fun touch(project: Project) {
      project.getService(DevToolsProjectWatcher::class.java)
    }
  }
}
