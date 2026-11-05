package dev.nowtech.builtin.devtools

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.IdeFrame
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.jcef.JBCefBrowserBase
import java.awt.Component
import java.awt.Container
import java.awt.KeyboardFocusManager
import javax.swing.JComponent

object DevToolsBrowserLocator {

  fun findActiveBrowser(project: Project?): JBCefBrowserBase? {
    JBCefBrowserBase.getFocusedBrowser()?.let { return it }

    findBrowserInComponent(KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner)?.let {
      return it
    }

    if (project != null && !project.isDefault) {
      val editor = FileEditorManager.getInstance(project).selectedEditor
      findBrowserInComponent(editor?.component)?.let { return it }
    }

    return null
  }

  fun projectForFocusedComponent(): Project? {
    val owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner ?: return null
    var component: Component? = owner
    while (component != null) {
      if (component is IdeFrame) {
        return component.project
      }
      component = component.parent
    }
    for (frame in WindowManager.getInstance().allProjectFrames) {
      val root = frame.component ?: continue
      if (root.isShowing && (root === owner || root.isAncestorOf(owner))) {
        return frame.project
      }
    }
    return null
  }

  private fun findBrowserInComponent(root: Component?): JBCefBrowserBase? {
    if (root == null) {
      return null
    }

    browserFromComponent(root)?.let { return it }

    if (root is Container) {
      for (child in root.components) {
        findBrowserInComponent(child)?.let { return it }
      }
    }

    return null
  }

  private fun browserFromComponent(component: Component): JBCefBrowserBase? {
    var current: Component? = component
    while (current != null) {
      if (current is JComponent) {
        val attached =
          current.getClientProperty(JBCefBrowserBase.JBCEFBROWSER_INSTANCE_PROP)
        if (attached is JBCefBrowserBase) {
          return attached
        }
      }
      current = current.parent
    }
    return null
  }
}
