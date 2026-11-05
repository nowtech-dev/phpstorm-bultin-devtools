package dev.nowtech.builtin.devtools

object DevToolsInspectorUrlPolisher {

  fun polish(raw: String): String {
    var url = raw
    url = stripQueryParam(url, "remoteFrontend")
    url = putQueryParam(url, "can_dock", "true")
    url = putQueryParam(url, "dockSide", "right")
    if (!url.contains("panel=")) {
      url = putQueryParam(url, "panel", "elements")
    }
    return url
  }

  private fun stripQueryParam(url: String, name: String): String {
    val regex = Regex("([?&])$name=[^&]*(&?)")
    return url.replace(regex) { match ->
      when {
        match.groupValues[1] == "?" && match.groupValues[2] == "&" -> "?"
        match.groupValues[2] == "&" -> match.groupValues[1]
        else -> ""
      }
    }.replace("?&", "?").trimEnd('?', '&')
  }

  private fun putQueryParam(url: String, name: String, value: String): String {
    if (Regex("([?&])$name=").containsMatchIn(url)) {
      return url
    }
    return url + (if (url.contains('?')) "&" else "?") + "$name=$value"
  }
}
