package dev.nowtech.builtin.devtools

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefBrowserBase
import java.awt.Component
import java.awt.Container
import javax.swing.JComponent

class DevToolsEditorHookListener : FileEditorManagerListener {

  override fun fileOpened(source: FileEditorManager, file: com.intellij.openapi.vfs.VirtualFile) {
    hookEditors(source.project)
  }

  override fun selectionChanged(event: com.intellij.openapi.fileEditor.FileEditorManagerEvent) {
    hookEditors(event.manager.project)
  }

  private fun hookEditors(project: Project) {
    val manager = FileEditorManager.getInstance(project)
    for (editor in manager.allEditors) {
      hookComponentTree(editor.component, project)
    }
    manager.selectedEditor?.component?.let { hookComponentTree(it, project) }
  }

  private fun hookComponentTree(root: Component, project: Project) {
    walk(root) { component ->
      browserFromComponent(component)?.let { DevToolsJcefHook.ensureHooked(it, project) }
    }
  }

  private fun walk(root: Component, visit: (Component) -> Unit) {
    visit(root)
    if (root is Container) {
      for (child in root.components) {
        walk(child, visit)
      }
    }
  }

  private fun browserFromComponent(component: Component): JBCefBrowserBase? {
    var current: Component? = component
    while (current != null) {
      if (current is JComponent) {
        val attached = current.getClientProperty(JBCefBrowserBase.JBCEFBROWSER_INSTANCE_PROP)
        if (attached is JBCefBrowserBase) {
          return attached
        }
      }
      current = current.parent
    }
    return null
  }
}
