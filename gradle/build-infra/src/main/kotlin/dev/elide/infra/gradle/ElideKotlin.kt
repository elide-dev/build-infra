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

import dev.elide.infra.gradle.kotlin.BaselineKotlinConventions
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * # Plugins: Kotlin Infrastructure
 *
 * Defines basic Kotlin infrastructure settings and configuration at the level of a Gradle project. This plug-in is
 * meant for use in `build-infra` child projects; it is not exported as a convention to downstream projects.
 */
public class ElideKotlin : Plugin<Project> {
  override fun apply(target: Project) {
    // start with kotlin multiplatform, and key extensions to the compiler
    target.pluginManager.apply(BuildConstants.KnownPlugins.KOTLIN_MULTIPLATFORM)

    target.pluginManager.withPlugin(BuildConstants.KnownPlugins.KOTLIN_MULTIPLATFORM) {
      target.pluginManager.apply(BaselineKotlinConventions::class.java)
    }
  }
}
