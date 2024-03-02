@file:Suppress("UnstableApiUsage")

package dev.elide.infra.gradle

import dev.elide.infra.gradle.api.ElideSettings
import dev.elide.infra.gradle.settings.BaselineBuildSettings
import dev.elide.infra.gradle.settings.BuildCaching
import dev.elide.infra.gradle.settings.PkgstRepositories
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import kotlin.reflect.KClass

// Provide the list of installed settings conventions.
internal fun settingsConventions(): List<KClass<out Plugin<Settings>>> = listOf(
  BaselineBuildSettings::class,
  BuildCaching::class,
  PkgstRepositories::class,
)

/**
 * # Baseline Plugin: Settings
 *
 * Describes an entrypoint plugin extension point, which applies all [baselineConventions] and then applies the current
 * plug-in implementation.
 */
public class SettingsUmbrella : Plugin<Settings> {
  override fun apply(target: Settings) {
    // apply all plugins
    target.pluginManager.apply {
      settingsConventions().forEach {
        apply(it)
      }
    }

    target.extensions.configure<ElideSettings.ElideSettingsDsl> {
      repositories.pkgst.convention(true)
      buildCaching.local.enabled.convention(true)
    }

    target.enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
    target.enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
  }
}

/**
 * ## Baseline Settings
 *
 * Initialize settings-time plugins which provide baseline settings to embedded build modules.
 */
public fun Settings.installBaselines() {
  // apply umbrella settings plugin
  plugins.apply(SettingsUmbrella::class)
}
