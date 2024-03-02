plugins {
  id(core.plugins.kotlin.multiplatform.get().pluginId) apply false
}

buildscript {
  dependencies {
    classpath(libs.guava)
  }

  repositories {
    maven {
      name = "pkgst-maven"
      url = uri("https://maven.pkg.st")
    }
    maven {
      name = "pkgst-gradle"
      url = uri("https://gradle.pkg.st")
    }
  }
}

dependencyLocking {
  lockAllConfigurations()
}

private fun Task.taskInAllBuilds(name: String) {
  dependsOn(gradle.includedBuilds.map { it.task(":$name") })
}

// Top-level tasks.

val clean by tasks.registering { taskInAllBuilds("clean") }
val build by tasks.registering { taskInAllBuilds("build") }
val test by tasks.registering { taskInAllBuilds("test") }
val check by tasks.registering { taskInAllBuilds("check") }
