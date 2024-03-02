package dev.elide.infra.gradle.settings

import build.less.plugin.gradle.BuildlessExtension
import dev.elide.infra.gradle.api.ElideSettings
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.kotlin.dsl.configure

/**
 * ## Settings: Build Caching
 */
public class BuildCaching : Plugin<Settings> {
  override fun apply(target: Settings) {
    target.extensions.getByType(ElideSettings.ElideSettingsDsl::class.java).let { conventions ->
      if (conventions.buildCaching.buildless.get()) target.extensions.configure<BuildlessExtension> {
        // Nothing at this time.
      }
      if (conventions.buildCaching.local.enabled.get()) {
        target.buildCache {
          local {
            isEnabled = true
            removeUnusedEntriesAfterDays = 14
            directory = ".codebase/build-cache"
          }
        }
      }
    }
  }
}
