package dev.nowtech.builtin.devtools

import com.intellij.util.ui.UIUtil
import java.awt.Color
import javax.swing.UIManager

data class DevToolsIdeColors(
  val background: String,
  val surface: String,
  val header: String,
  val tabSelected: String,
  val border: String,
  val foreground: String,
  val accent: String,
)

object DevToolsIdeTheme {

  fun read(): DevToolsIdeColors {
    val panel = UIUtil.getPanelBackground()
    val toolWindow = color("ToolWindow.background", panel)
    val header =
      color("ToolWindow.headerTab.selectedBackground", null)
        ?: color("ToolWindow.header.background", null)
        ?: color("MainToolbar.background", null)
        ?: blend(toolWindow, panel, 0.35f)
    val tabSelected =
      color("ToolWindow.headerTab.selectedBackground", null)
        ?: color("TabbedPane.selectedBackground", null)
        ?: lift(toolWindow, 0.08f)
    val border = color("Separator.color", null) ?: color("Component.borderColor", null) ?: UIUtil.getBoundsColor()
    val foreground = UIUtil.getLabelForeground()
    val accent = lift(toolWindow, 0.22f)

    return DevToolsIdeColors(
      background = hex(toolWindow),
      surface = hex(lift(toolWindow, 0.05f)),
      header = hex(header),
      tabSelected = hex(tabSelected),
      border = hex(border),
      foreground = hex(foreground),
      accent = hex(accent),
    )
  }

  fun themeCss(colors: DevToolsIdeColors): String =
    """
    :root {
      --sys-color-base: ${colors.background};
      --sys-color-cdt-base-container: ${colors.surface};
      --sys-color-base-container: ${colors.surface};
      --sys-color-surface: ${colors.background};
      --sys-color-surface-variant: ${colors.surface};
      --sys-color-tonal-container: ${colors.tabSelected};
      --sys-color-header-container: ${colors.header};
      --sys-color-primary: ${colors.accent};
      --sys-color-primary-bright: ${colors.accent};
      --sys-color-on-surface: ${colors.foreground};
      --sys-color-outline: ${colors.border};
    }
    html, body, .theme-with-dark-background, .-theme-with-dark-background,
    .widget-root, .inspected-target, .tabbed-pane-main-content, .main-content,
    .split-view-contents, .split-widget-contents, .elements-pane, .styles-pane,
    .console-view, .network-log-grid, .sources-pane, .pane-tabbed-pane,
    .toolbar, .toolbar-container, .editor-container, .widget.vbox, .widget.hbox
    { background-color: ${colors.background} !important; color: ${colors.foreground} !important; }
    .tabbed-pane-header, .tabbed-pane-header-tabs, .tabbed-pane-tab-bar,
    .tabbed-pane-header-container, [class*="tabbed-pane-header"]
    { background-color: ${colors.header} !important; background-image: none !important; border-color: ${colors.border} !important; }
    .tabbed-pane-header-tab, .tabbed-pane-tab, .tabbed-pane-header-tab-title
    { color: ${colors.foreground} !important; background-color: transparent !important; }
    .tabbed-pane-header-tab.selected, .tabbed-pane-tab.selected,
    .tabbed-pane-header-tab[aria-selected="true"], .tabbed-pane-tab[aria-selected="true"]
    { background-color: ${colors.tabSelected} !important; box-shadow: inset 0 -2px 0 ${colors.accent} !important; border-bottom-color: ${colors.accent} !important; }
    .sidebar-pane, .drawer-sidebar, .styles-sidebar-pane, .split-view-sidebar:not(:first-child)
    { background-color: ${colors.surface} !important; }
    .tree-outline li.selected, .data-grid-data-grid-node.selected
    { background-color: ${colors.tabSelected} !important; }
    """.trimIndent()

  private fun color(key: String, fallback: Color?): Color {
    val value = UIManager.getColor(key) ?: return fallback ?: UIUtil.getPanelBackground()
    return value
  }

  private fun hex(color: Color): String =
    String.format("#%02x%02x%02x", color.red, color.green, color.blue)

  private fun lift(base: Color, amount: Float): Color {
    val a = amount.coerceIn(0f, 1f)
    return Color(
      (base.red + (255 - base.red) * a).toInt().coerceIn(0, 255),
      (base.green + (255 - base.green) * a).toInt().coerceIn(0, 255),
      (base.blue + (255 - base.blue) * a).toInt().coerceIn(0, 255),
    )
  }

  private fun blend(a: Color, b: Color, ratio: Float): Color {
    val t = ratio.coerceIn(0f, 1f)
    return Color(
      (a.red * (1 - t) + b.red * t).toInt(),
      (a.green * (1 - t) + b.green * t).toInt(),
      (a.blue * (1 - t) + b.blue * t).toInt(),
    )
  }
}
