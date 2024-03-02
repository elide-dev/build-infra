@file:Suppress("UnstableApiUsage")

package dev.elide.infra.gradle.settings

import dev.elide.infra.gradle.BuildConstants
import dev.elide.infra.gradle.api.ElideSettings
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

private fun settingsPlugins(): List<String> = listOf(
  "build.less",
  "com.gradle.enterprise",
  "com.gradle.common-custom-user-data-gradle-plugin",
  "org.gradle.toolchains.foojay-resolver-convention",
)

/**
 * ## Settings: Baseline
 */
public class BaselineBuildSettings : Plugin<Settings> {
  override fun apply(target: Settings): Unit = target.apply {
    settingsPlugins().forEach { pluginId ->
      plugin(pluginId)
    }

    // create the baseline settings-time extension
    target.extensions.create(BuildConstants.Extensions.META, ElideSettings.ElideSettingsDsl::class.java)
    target.extensions.configure(ElideSettings.ElideSettingsDsl::class.java) {
      repositories.pkgst.convention(true)
      buildCaching.local.enabled.convention(true)
    }

    val catalogPath = when {
      target.rootDir.toPath().toString().contains("gradle/plugins/") -> "../../catalogs"
      target.rootDir.toPath().toString().contains("samples") -> "../catalogs"
      else -> "gradle/catalogs"
    }

    target.dependencyResolutionManagement {
      versionCatalogs {
        create("libs") {
          from(target.layout.rootDirectory.files( "$catalogPath/libs.versions.toml"))
        }
        create("core") {
          from(target.layout.rootDirectory.files("$catalogPath/core.versions.toml"))
        }
        create("infra") {
          from(target.layout.rootDirectory.files("$catalogPath/infra.versions.toml"))
        }
      }
    }
  }
}
