package dev.nowtech.builtin.devtools.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "BuiltInDevToolsSettings", storages = [Storage("builtInDevTools.xml")])
class DevToolsSettings : PersistentStateComponent<DevToolsSettings.State> {

  data class State(
    var enableContextMenuOnStartup: Boolean = true,
    /** @deprecated use [inspectorHost] */
    var openInExternalBrowser: Boolean = false,
    var inspectorHost: String = InspectorHost.IDE_TOOL_WINDOW.name,
    /** Hide screencast preview and target URL bar in the DevTools UI. */
    var compactInspectorUi: Boolean = true,
    /** Match DevTools chrome colors to the IDE theme (Darcula / New UI). */
    var matchIdeTheme: Boolean = true,
  )

  private var state = State()

  override fun getState(): State = state

  override fun loadState(state: State) {
    XmlSerializerUtil.copyBean(state, this.state)
  }

  fun resolveInspectorHost(): InspectorHost {
    runCatching { return InspectorHost.valueOf(state.inspectorHost) }
    return if (state.openInExternalBrowser) {
      InspectorHost.EXTERNAL_BROWSER
    } else {
      InspectorHost.IDE_TOOL_WINDOW
    }
  }

  companion object {
    fun getInstance(): DevToolsSettings =
      ApplicationManager.getApplication().getService(DevToolsSettings::class.java)
  }
}
