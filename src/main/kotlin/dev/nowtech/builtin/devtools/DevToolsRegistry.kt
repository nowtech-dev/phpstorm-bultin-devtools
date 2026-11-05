package dev.nowtech.builtin.devtools

import com.intellij.openapi.util.registry.Registry

object DevToolsRegistry {

  const val CONTEXT_MENU_KEY = "ide.browser.jcef.contextMenu.devTools.enabled"
  const val DEBUG_PORT_KEY = "ide.browser.jcef.debug.port"

  /** Native context menu DevTools uses IDE {@code openDevtools()} instead of this plugin's CDP flow. */
  fun disableNativeContextMenuDevTools(): Boolean {
    return runCatching {
      Registry.get(CONTEXT_MENU_KEY).setValue(false)
      devToolsLog.info("Built-in DevTools: disabled native registry $CONTEXT_MENU_KEY (uses plugin handler instead)")
      true
    }.getOrElse { e ->
      devToolsLog.warn("Built-in DevTools: failed to set registry $CONTEXT_MENU_KEY", e)
      false
    }
  }

  fun ensureDebugPort(defaultPort: Int = 9222): Boolean {
    return runCatching {
      val reg = Registry.get(DEBUG_PORT_KEY)
      if (reg.asInteger() <= 0) {
        reg.setValue(defaultPort)
        devToolsLog.info("Built-in DevTools: set registry $DEBUG_PORT_KEY=$defaultPort")
      }
      true
    }.getOrElse { e ->
      devToolsLog.warn("Built-in DevTools: failed to set registry $DEBUG_PORT_KEY", e)
      false
    }
  }
}
