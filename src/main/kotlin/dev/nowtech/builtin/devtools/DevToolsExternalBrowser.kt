package dev.nowtech.builtin.devtools

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.util.SystemInfo

object DevToolsExternalBrowser {

  fun open(url: String) {
    if (SystemInfo.isWindows) {
      runCatching {
        ProcessBuilder("cmd", "/c", "start", "", url).start()
      }.onSuccess { return }
    }
    BrowserUtil.browse(url)
  }
}
