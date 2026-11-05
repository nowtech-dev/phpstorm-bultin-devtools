package dev.nowtech.builtin.devtools

import dev.nowtech.builtin.devtools.settings.DevToolsSettings

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

  /** PhpStorm New UI tool window background (user preference). */
  private const val DEFAULT_BG = "#1E1F22"
  private const val SURFACE = "#2B2D30"
  private const val HEADER = "#2B2D30"
  private const val BORDER = "#43454A"
  private const val FOREGROUND = "#DFE1E5"
  private const val ACCENT = "#868A91"

  fun read(): DevToolsIdeColors {
    val raw = DevToolsSettings.getInstance().getState().devtoolsBackgroundHex
    val background = normalizeHex(raw.ifBlank { "1E1F22" })
    return DevToolsIdeColors(
      background = background,
      surface = SURFACE,
      header = HEADER,
      tabSelected = background,
      border = BORDER,
      foreground = FOREGROUND,
      accent = ACCENT,
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
      --sys-color-tonal-container: ${colors.surface};
      --sys-color-header-container: ${colors.header};
      --sys-color-primary: ${colors.accent};
      --sys-color-primary-bright: ${colors.accent};
      --sys-color-primary-variant: ${colors.accent};
      --sys-color-on-surface: ${colors.foreground};
      --sys-color-outline: ${colors.border};
      --color-background: ${colors.background};
      --color-background-opacity-50: ${colors.background};
      --color-background-opacity-80: ${colors.background};
      --color-primary: ${colors.accent};
      --color-primary-variant: ${colors.accent};
      --color-tab-selected: ${colors.tabSelected};
      --color-tab-hover: ${colors.surface};
    }
    html, body, .theme-with-dark-background, .-theme-with-dark-background,
    .widget-root, .inspected-target, .tabbed-pane-main-content, .main-content,
    .split-view-contents, .split-widget-contents, .elements-pane, .styles-pane,
    .console-view, .network-log-grid, .sources-pane, .pane-tabbed-pane,
    .editor-container, .widget.vbox, .widget.hbox, .panel, .panel-content
    { background-color: ${colors.background} !important; color: ${colors.foreground} !important; }
    .tabbed-pane-header, .tabbed-pane-header-tabs, .tabbed-pane-tab-bar,
    .tabbed-pane-header-container, .tabbed-pane-header-top, .top-toolbar-tabbed-pane,
    .toolbar-tabbed-pane, [class*="tabbed-pane-header"], [class*="TabBar"],
    header.devtools-tabbar, .tabbed-pane-header .tabbed-pane-header-tabs
    {
      background-color: ${colors.header} !important;
      background-image: none !important;
      box-shadow: none !important;
      border-color: ${colors.border} !important;
    }
    .tabbed-pane-header-tab, .tabbed-pane-tab, .tabbed-pane-header-tab-title,
    [role="tab"].tabbed-pane-header-tab
    {
      color: ${colors.foreground} !important;
      background-color: transparent !important;
      background-image: none !important;
    }
    .tabbed-pane-header-tab.selected, .tabbed-pane-tab.selected,
    .tabbed-pane-header-tab[aria-selected="true"], .tabbed-pane-tab[aria-selected="true"],
    [role="tab"][aria-selected="true"]
    {
      background-color: ${colors.tabSelected} !important;
      background-image: none !important;
      box-shadow: inset 0 -2px 0 ${colors.accent} !important;
      border-bottom: 2px solid ${colors.accent} !important;
      color: ${colors.foreground} !important;
    }
    .toolbar, .toolbar-container, .toolbar-button:not(:hover)
    { background-color: ${colors.background} !important; }
    .sidebar-pane, .drawer-sidebar, .styles-sidebar-pane
    { background-color: ${colors.surface} !important; }
    .tree-outline li.selected, .data-grid-data-grid-node.selected,
    .elements-tree .selected
    { background-color: ${colors.surface} !important; }
    """.trimIndent()

  fun themePatchJs(colors: DevToolsIdeColors): String =
    """
    (function() {
      var headerBg = '${colors.header}';
      var bodyBg = '${colors.background}';
      var accent = '${colors.accent}';
      document.querySelectorAll(
        '.tabbed-pane-header, .tabbed-pane-header-tabs, .tabbed-pane-tab-bar, .tabbed-pane-header-top'
      ).forEach(function(el) {
        el.style.setProperty('background-color', headerBg, 'important');
        el.style.setProperty('background-image', 'none', 'important');
      });
      document.querySelectorAll('.tabbed-pane-header-tab, .tabbed-pane-tab, [role="tab"]').forEach(function(tab) {
        tab.style.setProperty('background-image', 'none', 'important');
        if (tab.getAttribute('aria-selected') === 'true' || tab.classList.contains('selected')) {
          tab.style.setProperty('background-color', bodyBg, 'important');
          tab.style.setProperty('border-bottom-color', accent, 'important');
        }
      });
    })();
    """.trimIndent()

  private fun normalizeHex(raw: String): String {
    val trimmed = raw.trim().removePrefix("#")
    if (trimmed.length != 6 || trimmed.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
      return DEFAULT_BG
    }
    return "#$trimmed"
  }
}
