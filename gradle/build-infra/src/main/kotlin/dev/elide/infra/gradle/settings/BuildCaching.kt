/*
 * Copyright (c) 2024 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */
@file:Suppress("DEPRECATION")

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
