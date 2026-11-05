package dev.nowtech.builtin.devtools

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

class OpenBuiltInDevToolsAction : AnAction(), DumbAware {

  override fun actionPerformed(e: AnActionEvent) {
    val browser = DevToolsBrowserLocator.findActiveBrowser(e.project)
    if (browser == null) {
      notify(
        "No embedded browser focused",
        "Open a JCEF page (built-in HTML preview, browser tool window, etc.) and try again.",
        NotificationType.WARNING,
      )
      return
    }

    DevToolsJcefHook.ensureHooked(browser, e.project)
    DevToolsCdpOpener.open(e.project, browser)
  }

  private fun notify(title: String, content: String, type: NotificationType) {
    Notifications.Bus.notify(Notification("Built-in Browser DevTools", title, content, type))
  }
}
