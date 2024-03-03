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

package dev.elide.infra.gradle.baseline

import dev.elide.infra.gradle.BuildConstants
import dev.elide.infra.gradle.api.ElideBuild
import org.gradle.api.Plugin
import org.gradle.api.Project

private fun allProjectsPlugins(): List<String> = listOf(
  // None yet.
)

/**
 * # Conventions: Baseline
 */
public class BaselineConventions : Plugin<Project> {
  override fun apply(target: Project) {
    target.group = "dev.elide.infra"

    allProjectsPlugins().forEach { pluginId ->
      target.pluginManager.apply(pluginId)
    }

    // create the baseline settings-time extension
    if (target.extensions.findByName(BuildConstants.Extensions.META) == null)
      target.extensions.create(BuildConstants.Extensions.META, ElideBuild.ElideBuildDsl::class.java)
  }
}
