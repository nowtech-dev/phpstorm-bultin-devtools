package dev.nowtech.builtin.devtools

import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.jcef.JBCefBrowser
import org.cef.browser.CefBrowser
import org.cef.handler.CefLoadHandlerAdapter
import dev.nowtech.builtin.devtools.settings.DevToolsSettings

object DevToolsCompactUi {

  private const val MARKER = "dev.nowtech.builtin.devtools.uiHook"

  fun attachIfNeeded(browser: JBCefBrowser) {
    val settings = DevToolsSettings.getInstance().getState()
    if (!settings.compactInspectorUi && !settings.matchIdeTheme) {
      return
    }
    if (browser.getProperty(MARKER) == true) {
      return
    }
    browser.setProperty(MARKER, true)
    val cef = browser.cefBrowser ?: return
    browser.getJBCefClient().addLoadHandler(
      object : CefLoadHandlerAdapter() {
        override fun onLoadingStateChange(
          cefBrowser: CefBrowser,
          isLoading: Boolean,
          canGoBack: Boolean,
          canGoForward: Boolean,
        ) {
          if (isLoading) {
            return
          }
          ApplicationManager.getApplication().invokeLater {
            val script = buildInjectionScript()
            cefBrowser.executeJavaScript(script, cefBrowser.url, 0)
          }
        }
      },
      cef,
    )
  }

  private fun buildInjectionScript(): String {
    val settings = DevToolsSettings.getInstance().getState()
    val ideColors = if (settings.matchIdeTheme) DevToolsIdeTheme.read() else null
    val themeCss = ideColors?.let { DevToolsIdeTheme.themeCss(it) } ?: ""
    val themePatchJs = ideColors?.let { DevToolsIdeTheme.themePatchJs(it) } ?: ""
    val compact = settings.compactInspectorUi
    return """
    (function() {
      var themeCss = ${escapeJsString(themeCss)};
      var themePatchJs = ${escapeJsString(themePatchJs)};
      var compact = $compact;

      function isScreencastNode(el) {
        if (!el) return false;
        var cn = (el.className && el.className.toString()) || '';
        if (/screencast/i.test(cn)) return true;
        if (el.tagName && el.tagName.toLowerCase() === 'devtools-screencast') return true;
        return el.querySelector && el.querySelector('[class*="screencast" i], devtools-screencast') != null;
      }

      function hideRemotePreviewOnly() {
        document.querySelectorAll('[class*="screencast" i], devtools-screencast').forEach(function(el) {
          el.style.setProperty('display', 'none', 'important');
          el.style.setProperty('width', '0', 'important');
          el.style.setProperty('height', '0', 'important');
        });
        document.querySelectorAll('.inspected-target > .split-widget-vertical, .inspected-target > .split-view-vertical').forEach(function(root) {
          var first = root.firstElementChild;
          if (first && isScreencastNode(first)) {
            first.style.setProperty('display', 'none', 'important');
            first.style.setProperty('width', '0', 'important');
            first.style.setProperty('flex', '0 0 0', 'important');
          }
          for (var i = 0; i < root.children.length; i++) {
            var c = root.children[i];
            var cn = (c.className && c.className.toString()) || '';
            if (/resizer/i.test(cn)) {
              c.style.setProperty('display', 'none', 'important');
            } else if (c !== first || !isScreencastNode(first)) {
              c.style.setProperty('flex', '1 1 100%', 'important');
              c.style.setProperty('width', '100%', 'important');
              c.style.setProperty('max-width', '100%', 'important');
            }
          }
        });
        document.querySelectorAll('[aria-label*="Screencast" i], [title*="Screencast" i]').forEach(function(el) {
          if (el.getAttribute('aria-pressed') === 'true' && el.click) el.click();
        });
      }

      function applyUi() {
        var styleId = 'nowtech-devtools-ui';
        var css = document.getElementById(styleId);
        if (!css) {
          css = document.createElement('style');
          css.id = styleId;
          document.documentElement.appendChild(css);
        }
        var parts = [];
        if (themeCss) parts.push(themeCss);
        if (compact) {
          parts.push([
            'html, body { width: 100% !important; height: 100% !important; margin: 0 !important; }',
            '[class*="screencast" i], devtools-screencast,',
            '.device-mode-toolbar, .toolbar-input, .toolbar-url, .text-prompt-root,',
            '[class*="InspectedPage" i], [class*="inspected-url" i], [class*="address-bar" i]',
            '{ display: none !important; visibility: hidden !important; height: 0 !important; max-height: 0 !important; }',
            '.inspected-target > .split-widget-vertical, .inspected-target > .split-view-vertical',
            '{ width: 100% !important; max-width: 100% !important; flex: 1 1 auto !important; }'
          ].join(' '));
        }
        css.textContent = parts.join('\\n');
        if (compact) hideRemotePreviewOnly();
        if (themePatchJs) {
          try { eval(themePatchJs); } catch (e) {}
        }
      }

      function touchUi() {
        if (compact) hideRemotePreviewOnly();
        if (themePatchJs) { try { eval(themePatchJs); } catch (e) {} }
      }

      applyUi();
      if (!window.__nowtechDevToolsUiObserver) {
        window.__nowtechDevToolsUiObserver = new MutationObserver(touchUi);
        window.__nowtechDevToolsUiObserver.observe(document.documentElement, { childList: true, subtree: true });
      }
    })();
    """.trimIndent()
  }

  private fun escapeJsString(value: String): String {
    val escaped =
      value
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\r", "")
        .replace("\n", "\\n")
    return "'$escaped'"
  }
}
