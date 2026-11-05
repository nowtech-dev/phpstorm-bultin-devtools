package dev.nowtech.builtin.devtools.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import dev.nowtech.builtin.devtools.DevToolsRegistry
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JComponent

class DevToolsConfigurable : Configurable {

  private var enableRegistryBox: JBCheckBox? = null
  private var toolWindowRadio: JBRadioButton? = null
  private var externalRadio: JBRadioButton? = null
  private var compactUiBox: JBCheckBox? = null
  private var matchThemeBox: JBCheckBox? = null
  private var backgroundHexField: JBTextField? = null

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
    compactUiBox =
      JBCheckBox(
        "Compact DevTools (hide page preview and URL bar; panels only)",
        settings.getState().compactInspectorUi,
      )
    matchThemeBox =
      JBCheckBox(
        "Match DevTools colors to PhpStorm theme (neutral tab bar, no Chrome blue)",
        settings.getState().matchIdeTheme,
      )
    backgroundHexField =
      JBTextField(settings.getState().devtoolsBackgroundHex, 8).apply {
        emptyText.text = "1E1F22"
      }
    val applyRegistryButton =
      JButton("Apply registry now").apply {
        addActionListener { DevToolsRegistry.disableNativeContextMenuDevTools() }
      }
    return FormBuilder.createFormBuilder()
      .addComponent(enableRegistryBox!!)
      .addComponent(toolWindowRadio!!)
      .addComponent(externalRadio!!)
      .addComponent(compactUiBox!!)
      .addComponent(matchThemeBox!!)
      .addLabeledComponent("DevTools background (#RRGGBB)", backgroundHexField!!)
      .addComponent(
        JBLabel(
          "<html>Uses the CDP <code>devtoolsFrontendUrl</code> (Chrome DevTools on appspot) when available. " +
            "Tab bar uses neutral gray <code>#2B2D30</code>; active tab underline <code>#868A91</code>.</html>",
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
      host != DevToolsSettings.getInstance().resolveInspectorHost() ||
      compactUiBox?.isSelected != s.compactInspectorUi ||
      matchThemeBox?.isSelected != s.matchIdeTheme ||
      backgroundHexField?.text?.trim()?.removePrefix("#") != s.devtoolsBackgroundHex.trim().removePrefix("#")
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
    s.compactInspectorUi = compactUiBox?.isSelected == true
    s.matchIdeTheme = matchThemeBox?.isSelected == true
    s.devtoolsBackgroundHex = backgroundHexField?.text?.trim()?.removePrefix("#") ?: "1E1F22"
  }

  override fun reset() {
    val s = DevToolsSettings.getInstance().getState()
    enableRegistryBox?.isSelected = s.enableContextMenuOnStartup
    when (DevToolsSettings.getInstance().resolveInspectorHost()) {
      InspectorHost.IDE_TOOL_WINDOW -> toolWindowRadio?.isSelected = true
      InspectorHost.EXTERNAL_BROWSER -> externalRadio?.isSelected = true
    }
    compactUiBox?.isSelected = s.compactInspectorUi
    matchThemeBox?.isSelected = s.matchIdeTheme
    backgroundHexField?.text = s.devtoolsBackgroundHex
  }
}
