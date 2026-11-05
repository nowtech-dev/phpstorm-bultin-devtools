package dev.nowtech.builtin.devtools

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.util.io.HttpRequests

object DevToolsCdpOpener {

  private val gson = Gson()

  data class DebugTarget(
    val id: String? = null,
    val type: String? = null,
    val title: String? = null,
    val url: String? = null,
    val webSocketDebuggerUrl: String? = null,
    val devtoolsFrontendUrl: String? = null,
  )

  fun open(project: Project?, browser: JBCefBrowserBase) {
    if (!JBCefApp.isSupported()) {
      notify(
        "DevTools failed",
        "JCEF is not supported in this IDE runtime.",
        NotificationType.ERROR,
      )
      return
    }

    val pageUrl = browser.cefBrowser?.url.orEmpty()

    ApplicationManager.getApplication().invokeLater {
      JBCefApp.getInstance().getRemoteDebuggingPort { port ->
        if (port == null || port <= 0) {
          notify(
            "DevTools failed",
            "JCEF remote debug port is not available. Restart the IDE and ensure " +
              "Registry → ide.browser.jcef.debug.port is set (default 9222).",
            NotificationType.ERROR,
          )
          return@getRemoteDebuggingPort
        }

        ApplicationManager.getApplication().executeOnPooledThread {
          val result =
            runCatching {
              val targets = fetchTargets(port)
              devToolsLog.info(
                "Built-in DevTools: CDP /json returned ${targets.size} targets; pageUrl=$pageUrl",
              )
              val target = pickTarget(targets, pageUrl)
                ?: throw IllegalStateException(
                  "No debug target for this page. Reload the preview and try again.\nPage: $pageUrl",
                )
              val ws = target.webSocketDebuggerUrl
                ?: throw IllegalStateException("Debug target has no webSocketDebuggerUrl")
              resolveInspectorUrl(port, target, ws)
            }

          ApplicationManager.getApplication().invokeLater {
            result.fold(
              onSuccess = { inspectorUrl ->
                openInspectorUi(project, inspectorUrl)
                devToolsLog.info("Built-in DevTools: opening inspector at $inspectorUrl")
              },
              onFailure = { err ->
                devToolsLog.warn("Built-in DevTools: CDP open failed", err)
                notify(
                  "DevTools failed",
                  err.message ?: err.toString(),
                  NotificationType.ERROR,
                )
              },
            )
          }
        }
      }
    }
  }

  private fun fetchTargets(port: Int): List<DebugTarget> {
    val json = HttpRequests.request("http://127.0.0.1:$port/json").readString()
    val type = object : TypeToken<List<DebugTarget>>() {}.type
    return gson.fromJson(json, type) ?: emptyList()
  }

  private fun pickTarget(targets: List<DebugTarget>, pageUrl: String): DebugTarget? {
    val pages = targets.filter { it.type == "page" }
    if (pages.isEmpty()) {
      return null
    }
    if (pageUrl.isBlank()) {
      return pages.first()
    }
    val want = normalizeUrl(pageUrl)
    return pages.firstOrNull { normalizeUrl(it.url.orEmpty()) == want }
      ?: pages.firstOrNull {
        val got = normalizeUrl(it.url.orEmpty())
        got.contains(want) || want.contains(got)
      }
      ?: pages.firstOrNull()
  }

  private fun normalizeUrl(raw: String): String {
    return raw.trim().lowercase().replace('\\', '/').trimEnd('/')
  }

  internal fun resolveInspectorUrl(port: Int, target: DebugTarget, webSocketDebuggerUrl: String): String {
    target.devtoolsFrontendUrl?.let { raw ->
      when {
        raw.startsWith("https://") || raw.startsWith("http://") -> return raw
        raw.startsWith("/") -> return "http://127.0.0.1:$port$raw"
        else -> return "http://127.0.0.1:$port/$raw"
      }
    }
    val wsPath = webSocketDebuggerUrl.removePrefix("ws://").removePrefix("wss://")
    return "http://127.0.0.1:$port/devtools/inspector.html?experiments=true&ws=$wsPath"
  }

  private fun openInspectorUi(project: Project?, inspectorUrl: String) {
    DevToolsInspectorUi.open(project, inspectorUrl)
  }

  private fun notify(title: String, content: String, type: NotificationType, link: String? = null) {
    val notification = Notification("Built-in Browser DevTools", title, content, type)
    if (link != null) {
      notification.addAction(
        NotificationAction.createSimple("Copy inspector URL") {
          CopyPasteManager.getInstance().setContents(StringSelection(link))
        },
      )
    }
    Notifications.Bus.notify(notification)
  }
}
