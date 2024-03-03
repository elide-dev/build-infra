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

package dev.elide.infra.gradle

import dev.elide.infra.gradle.baseline.BaselineConventions
import dev.elide.infra.gradle.baseline.DependencyConventions
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import kotlin.reflect.KClass

// Provide the list of installed baseline conventions.
internal fun baselineConventions(): List<KClass<out Plugin<Project>>> = listOf(
  BaselineConventions::class,
  DependencyConventions::class,
)

/**
 * # Baseline Plugin
 *
 * Describes an entrypoint plugin extension point, which applies all [baselineConventions] and then applies the current
 * plug-in implementation.
 */
public class BaselinePlugin : Plugin<Project> {
  override fun apply(target: Project) {
    // apply all plugins
    target.pluginManager.apply {
      baselineConventions().forEach {
        apply(it)
      }
    }

    // activate the current plugin
    target.activate()
  }

  /**
   * ## Activate Plugin
   *
   * Activate this plugin after dependency plugins are applied.
   *
   * @receiver Active project
   */
  public fun Project.activate() {
    // no-op
  }
}
