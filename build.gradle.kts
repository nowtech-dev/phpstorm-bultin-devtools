plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "2.2.20"
  id("org.jetbrains.intellij.platform") version "2.4.0"
}

group = "dev.nowtech"
version = "1.2.2"

kotlin {
  jvmToolchain(21)
}

val phpstormHome = providers.gradleProperty("phpstorm.home").get()

repositories {
  mavenCentral()
  intellijPlatform {
    defaultRepositories()
  }
}

dependencies {
  intellijPlatform {
    local(phpstormHome)
  }

  val ideHome = file(phpstormHome)
  val jcefLib = ideHome.resolve("plugins/jcef-plugin/lib")
  val platformLib = ideHome.resolve("lib")
  compileOnly(
    files(
      jcefLib.resolve("modules/intellij.platform.ui.jcef.jar"),
      jcefLib.resolve("modules/intellij.platform.ide.impl.jcef.jar"),
      jcefLib.resolve("modules/intellij.libraries.jcef.jar"),
      platformLib.resolve("util-8.jar"),
    ),
  )
  implementation("com.google.code.gson:gson:2.11.0")
}

intellijPlatform {
  pluginConfiguration {
    id = "dev.nowtech.builtin.devtools"
    name = "Built-in Browser DevTools"
    version = project.version.toString()
    ideaVersion {
      sinceBuild = "262.10968"
      untilBuild = "262.*"
    }
    vendor {
      name = "NOW Tech"
      url = "https://phpstorm.nowtech.dev"
    }
  }
}

tasks {
  buildSearchableOptions {
    enabled = false
  }
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
      freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
  }
}
