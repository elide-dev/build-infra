@file:Suppress("UnstableApiUsage")

package dev.elide.infra.gradle.settings

import dev.elide.infra.gradle.api.ElideSettings
import org.gradle.api.Plugin
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.initialization.Settings
import org.gradle.api.initialization.resolve.RepositoriesMode
import org.gradle.api.initialization.resolve.RulesMode
import java.net.URI

private fun RepositoryHandler.infraRepositories() {
  maven {
    name = "pkgst-gradle"
    url = URI.create("https://gradle.pkg.st")
  }
  maven {
    name = "pkgst-maven"
    url = URI.create("https://maven.pkg.st")
  }
  mavenCentral {
    name = "maven-central"
  }
  gradlePluginPortal {
    name = "gradle-plugins"
  }
}

/**
 * ## Settings: Pkgst Repositories
 */
public class PkgstRepositories : Plugin<Settings> {
  override fun apply(target: Settings) {
    target.extensions.getByType(ElideSettings.ElideSettingsDsl::class.java).let { conventions ->
      if (conventions.repositories.pkgst.get()) {
        target.pluginManagement {
          repositories {
            infraRepositories()
          }
        }

        target.dependencyResolutionManagement {
          rulesMode.set(RulesMode.PREFER_SETTINGS)
          repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

          repositories {
            infraRepositories()
          }
        }
      }
    }
  }
}
