package dev.nowtech.builtin.devtools

import com.intellij.ui.jcef.JBCefBrowser
import org.cef.browser.CefBrowser
import org.cef.handler.CefLoadHandlerAdapter
import dev.nowtech.builtin.devtools.settings.DevToolsSettings

object DevToolsCompactUi {

  private const val MARKER = "dev.nowtech.builtin.devtools.compactUiHook"

  private val compactScript =
    """
    (function() {
      function expandMainPanel(root) {
        if (!root || !root.children) return;
        for (var i = 0; i < root.children.length; i++) {
          var c = root.children[i];
          var cn = (c.className && c.className.toString()) || '';
          if (cn.indexOf('screencast') >= 0 || cn.indexOf('Screencast') >= 0 ||
              (c.classList && c.classList.contains('split-view-sidebar'))) {
            c.style.setProperty('display', 'none', 'important');
            c.style.setProperty('width', '0', 'important');
            c.style.setProperty('flex', '0 0 0', 'important');
          } else if (cn.indexOf('resizer') >= 0 || cn.indexOf('Resizer') >= 0) {
            c.style.setProperty('display', 'none', 'important');
          } else {
            c.style.setProperty('flex', '1 1 100%', 'important');
            c.style.setProperty('width', '100%', 'important');
            c.style.setProperty('max-width', '100%', 'important');
            c.style.setProperty('min-width', '0', 'important');
          }
        }
      }

      function hideChrome() {
        var styleId = 'nowtech-devtools-compact';
        if (!document.getElementById(styleId)) {
          var css = document.createElement('style');
          css.id = styleId;
          css.textContent = [
            'html, body { width: 100% !important; height: 100% !important; margin: 0 !important; overflow: hidden !important; }',
            '[class*="screencast" i], [class*="Screencast" i], devtools-screencast,',
            '.device-mode-toolbar, .toolbar-input, .toolbar-url, .text-prompt-root,',
            '[class*="InspectedPage" i], [class*="inspected-url" i], [class*="address-bar" i]',
            '{ display: none !important; visibility: hidden !important; height: 0 !important; max-height: 0 !important; }',
            '.split-widget.split-widget-vertical > .split-view-sidebar,',
            '.split-view.split-view-vertical > .split-view-sidebar,',
            '.split-widget.split-widget-vertical > .split-view-sidebar:first-child,',
            '.split-view.split-view-vertical > .split-view-sidebar:first-child',
            '{ display: none !important; width: 0 !important; min-width: 0 !important; flex: 0 0 0 !important; }',
            '.split-widget.split-widget-vertical > .split-view-resizer,',
            '.split-view.split-view-vertical > .split-view-resizer',
            '{ display: none !important; width: 0 !important; }',
            '.split-widget.split-widget-vertical, .split-view.split-view-vertical,',
            '.widget-root, .inspected-target, .tabbed-pane-main-content, .main-content, #main-content',
            '{ width: 100% !important; max-width: 100% !important; flex: 1 1 auto !important; }',
            '.split-widget.split-widget-vertical > *:not(.split-view-sidebar):not(.split-view-resizer),',
            '.split-view.split-view-vertical > *:not(.split-view-sidebar):not(.split-view-resizer)',
            '{ flex: 1 1 100% !important; width: 100% !important; max-width: 100% !important; min-width: 0 !important; }'
          ].join(' ');
          document.documentElement.appendChild(css);
        }
        document.querySelectorAll('.split-widget-vertical, .split-view-vertical').forEach(expandMainPanel);
        document.querySelectorAll('[aria-label*="Screencast" i], [title*="Screencast" i]').forEach(function(el) {
          if (el.getAttribute('aria-pressed') === 'true' && el.click) el.click();
        });
      }
      hideChrome();
      if (!window.__nowtechDevToolsCompactObserver) {
        window.__nowtechDevToolsCompactObserver = new MutationObserver(hideChrome);
        window.__nowtechDevToolsCompactObserver.observe(document.documentElement, { childList: true, subtree: true, attributes: true });
      }
    })();
    """.trimIndent()

  fun attachIfNeeded(browser: JBCefBrowser) {
    if (!DevToolsSettings.getInstance().getState().compactInspectorUi) {
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
          cefBrowser.executeJavaScript(compactScript, cefBrowser.url, 0)
        }
      },
      cef,
    )
  }
}
