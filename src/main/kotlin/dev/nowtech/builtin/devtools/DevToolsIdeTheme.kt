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

  fun themeCss(colors: DevToolsIdeColors): String = headerAndChromeCss(colors) + panelCss(colors)

  private fun headerAndChromeCss(colors: DevToolsIdeColors): String =
    """
    :host, :root,
    html.theme-with-dark-background,
    html.baseline-default.theme-with-dark-background,
    html.baseline-grayscale.theme-with-dark-background
    {
      --sys-color-base: ${colors.background};
      --sys-color-cdt-base-container: ${colors.header};
      --sys-color-base-container: ${colors.surface};
      --sys-color-surface: ${colors.background};
      --sys-color-surface-variant: ${colors.surface};
      --sys-color-tonal-container: ${colors.header};
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
    .tabbed-pane-header, .tabbed-pane-header-contents, .tabbed-pane-header-tabs,
    .tabbed-pane-header-container, .tabbed-pane-header-top, .tabbed-pane-tab-bar,
    .top-toolbar-tabbed-pane, .toolbar-tabbed-pane, [class*="tabbed-pane-header"],
    .tabbed-pane-tab-slider, #elements-crumbs, .toolbar, .toolbar-container
    {
      background-color: ${colors.header} !important;
      background-image: none !important;
      box-shadow: none !important;
      border-color: ${colors.border} !important;
    }
    .tabbed-pane-header-tab, .tabbed-pane-tab, .tabbed-pane-header-tab-title,
    [role="tab"]
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
    .tabbed-pane-tab-slider { background-color: ${colors.accent} !important; }
    """.trimIndent()

  private fun panelCss(colors: DevToolsIdeColors): String =
    """
    html, body, .theme-with-dark-background, .-theme-with-dark-background,
    .widget-root, .inspected-target, .tabbed-pane-main-content, .main-content,
    .split-view-contents, .split-widget-contents, .elements-pane, .styles-pane,
    .console-view, .network-log-grid, .sources-pane, .pane-tabbed-pane,
    .editor-container, .widget.vbox, .widget.hbox, .panel, .panel-content
    { background-color: ${colors.background} !important; color: ${colors.foreground} !important; }
    .sidebar-pane, .drawer-sidebar, .styles-sidebar-pane
    { background-color: ${colors.surface} !important; }
    .tree-outline li.selected, .data-grid-data-grid-node.selected
    { background-color: ${colors.surface} !important; }
    """.trimIndent()

  /** Runs in the DevTools page; pierces nested shadow roots. */
  fun themePatchJs(colors: DevToolsIdeColors): String {
    val headerCss = escapeForJs(headerAndChromeCss(colors))
    return """
    (function() {
      var headerBg = '${colors.header}';
      var bodyBg = '${colors.background}';
      var accent = '${colors.accent}';
      var headerCss = '$headerCss';
      var styleId = 'nowtech-devtools-shadow-theme';

      function forceNeutralChromeTheme() {
        var html = document.documentElement;
        if (html) {
          html.classList.remove('baseline-default');
          html.classList.add('baseline-grayscale', 'theme-with-dark-background');
        }
        var tokenTargets = [html, document.body];
        tokenTargets.forEach(function(el) {
          if (!el || !el.style) return;
          el.style.setProperty('--user-color-source', 'baseline-grayscale', 'important');
          el.style.setProperty('--sys-color-tonal-container', headerBg, 'important');
          el.style.setProperty('--sys-color-cdt-base-container', headerBg, 'important');
          el.style.setProperty('--sys-color-header-container', headerBg, 'important');
          el.style.setProperty('--sys-color-base-container', bodyBg, 'important');
        });
      }

      function collectRoots() {
        var roots = [document];
        var stack = [document];
        while (stack.length) {
          var root = stack.pop();
          root.querySelectorAll('*').forEach(function(el) {
            if (el.shadowRoot) {
              roots.push(el.shadowRoot);
              stack.push(el.shadowRoot);
            }
          });
        }
        return roots;
      }

      function injectStyle(root) {
        if (!root) return;
        var st = root.querySelector('#' + styleId);
        if (!st) {
          st = document.createElement('style');
          st.id = styleId;
          root.appendChild(st);
        }
        st.textContent = headerCss;
      }

      function paintBar(el) {
        el.style.setProperty('background-color', headerBg, 'important');
        el.style.setProperty('background-image', 'none', 'important');
        el.style.setProperty('box-shadow', 'none', 'important');
        el.style.setProperty('border-color', 'transparent', 'important');
      }

      function isChromeBlue(color) {
        var m = /rgba?\((\d+),\s*(\d+),\s*(\d+)/.exec(color || '');
        if (!m) return false;
        var r = +m[1], g = +m[2], b = +m[3];
        if (b > 55 && b > r + 8) return true;
        if (r < 90 && g > 50 && b > 50) return true;
        return false;
      }

      function patchTabChrome() {
        forceNeutralChromeTheme();
        collectRoots().forEach(function(root) {
          injectStyle(root);
          root.querySelectorAll(
            '.tabbed-pane-header, .tabbed-pane-header-contents, .tabbed-pane-header-tabs, ' +
            '.tabbed-pane-tab-slider, #elements-crumbs, [class*="tabbed-pane-header"], ' +
            '.toolbar, .toolbar-container'
          ).forEach(paintBar);
          root.querySelectorAll('*').forEach(function(el) {
            if (!el.getBoundingClientRect) return;
            var rect = el.getBoundingClientRect();
            if (rect.top > 72 || rect.height < 8 || rect.width < 24) return;
            var cn = (el.className && el.className.toString()) || '';
            if (/tabbed-pane|toolbar|header|crumb|navigation/i.test(cn) || el.getAttribute('role') === 'tab') {
              paintBar(el);
            } else if (isChromeBlue(window.getComputedStyle(el).backgroundColor)) {
              paintBar(el);
            }
          });
        });
        document.querySelectorAll('[role="tab"]').forEach(function(tab) {
          tab.style.setProperty('background-image', 'none', 'important');
          if (tab.getAttribute('aria-selected') === 'true' || tab.classList.contains('selected')) {
            tab.style.setProperty('background-color', bodyBg, 'important');
            tab.style.setProperty('border-bottom-color', accent, 'important');
          }
        });
      }

      patchTabChrome();
      if (!window.__nowtechBaselineObserver) {
        window.__nowtechBaselineObserver = new MutationObserver(function() {
          forceNeutralChromeTheme();
        });
        window.__nowtechBaselineObserver.observe(document.documentElement, {
          attributes: true,
          attributeFilter: ['class']
        });
        document.addEventListener('themechange', forceNeutralChromeTheme, true);
      }
      if (!window.__nowtechTabPatchTimer) {
        window.__nowtechTabPatchTimer = setInterval(patchTabChrome, 750);
      }
    })();
    """.trimIndent()
  }

  private fun escapeForJs(css: String): String =
    css
      .replace("\\", "\\\\")
      .replace("'", "\\'")
      .replace("\r", "")
      .replace("\n", " ")

  private fun normalizeHex(raw: String): String {
    val trimmed = raw.trim().removePrefix("#")
    if (trimmed.length != 6 || trimmed.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) {
      return DEFAULT_BG
    }
    return "#$trimmed"
  }
}
