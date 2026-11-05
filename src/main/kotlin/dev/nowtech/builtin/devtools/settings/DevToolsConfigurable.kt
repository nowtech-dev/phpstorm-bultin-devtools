package dev.nowtech.builtin.devtools.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBRadioButton
import com.intellij.util.ui.FormBuilder
import dev.nowtech.builtin.devtools.DevToolsRegistry
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JComponent

class DevToolsConfigurable : Configurable {

  private var enableRegistryBox: JBCheckBox? = null
  private var toolWindowRadio: JBRadioButton? = null
  private var externalRadio: JBRadioButton? = null

  override fun getDisplayName(): String = "Built-in Browser DevTools"

  override fun createComponent(): JComponent {
    val settings = DevToolsSettings.getInstance()
    enableRegistryBox =
      JBCheckBox(
        "Replace JetBrains \"Open DevTools\" (F12 + context menu; disables blank CEF window)",
        settings.getState().enableContextMenuOnStartup,
      )
    toolWindowRadio = JBRadioButton("Open in IDE tool window (bottom, like Terminal)", true)
    externalRadio = JBRadioButton("Open in default browser (Chrome/Edge)", false)
    ButtonGroup().apply {
      add(toolWindowRadio)
      add(externalRadio)
    }
    when (settings.resolveInspectorHost()) {
      InspectorHost.IDE_TOOL_WINDOW -> toolWindowRadio?.isSelected = true
      InspectorHost.EXTERNAL_BROWSER -> externalRadio?.isSelected = true
    }
    val applyRegistryButton =
      JButton("Apply registry now").apply {
        addActionListener { DevToolsRegistry.disableNativeContextMenuDevTools() }
      }
    return FormBuilder.createFormBuilder()
      .addComponent(enableRegistryBox!!)
      .addComponent(toolWindowRadio!!)
      .addComponent(externalRadio!!)
      .addComponent(
        JBLabel(
          "<html>Uses the CDP <code>devtoolsFrontendUrl</code> (Chrome DevTools on appspot) when available. " +
            "Local <code>127.0.0.1/devtools/…</code> is fallback only.</html>",
        ),
      )
      .addComponent(applyRegistryButton)
      .panel
  }

  override fun isModified(): Boolean {
    val s = DevToolsSettings.getInstance().getState()
    val host =
      if (toolWindowRadio?.isSelected == true) {
        InspectorHost.IDE_TOOL_WINDOW
      } else {
        InspectorHost.EXTERNAL_BROWSER
      }
    return enableRegistryBox?.isSelected != s.enableContextMenuOnStartup ||
      host != DevToolsSettings.getInstance().resolveInspectorHost()
  }

  override fun apply() {
    val s = DevToolsSettings.getInstance().getState()
    s.enableContextMenuOnStartup = enableRegistryBox?.isSelected == true
    s.inspectorHost =
      if (toolWindowRadio?.isSelected == true) {
        InspectorHost.IDE_TOOL_WINDOW.name
      } else {
        InspectorHost.EXTERNAL_BROWSER.name
      }
    s.openInExternalBrowser = s.inspectorHost == InspectorHost.EXTERNAL_BROWSER.name
  }

  override fun reset() {
    val s = DevToolsSettings.getInstance().getState()
    enableRegistryBox?.isSelected = s.enableContextMenuOnStartup
    when (DevToolsSettings.getInstance().resolveInspectorHost()) {
      InspectorHost.IDE_TOOL_WINDOW -> toolWindowRadio?.isSelected = true
      InspectorHost.EXTERNAL_BROWSER -> externalRadio?.isSelected = true
    }
  }
}
