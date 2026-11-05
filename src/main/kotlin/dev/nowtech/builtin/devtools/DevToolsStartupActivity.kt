package dev.nowtech.builtin.devtools

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import dev.nowtech.builtin.devtools.settings.DevToolsSettings

class DevToolsStartupActivity : StartupActivity {

  override fun runActivity(project: Project) {
    DevToolsApplicationService.getInstance()

    synchronized(RegistryGate) {
      if (!RegistryGate.registryApplied) {
        RegistryGate.registryApplied = true
        DevToolsRegistry.ensureDebugPort()
        if (DevToolsSettings.getInstance().getState().enableContextMenuOnStartup) {
          DevToolsRegistry.disableNativeContextMenuDevTools()
        }
      }
    }

    DevToolsProjectWatcher.touch(project)
    DevToolsJcefHook.installForProject(project)
    DevToolsApplicationService.getInstance().scanAllWindows()
  }

  private object RegistryGate {
    @Volatile var registryApplied = false
  }
}
