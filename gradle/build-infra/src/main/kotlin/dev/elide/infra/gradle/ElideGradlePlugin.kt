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
import dev.elide.infra.gradle.testing.ElidePluginTestingConventions
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * # Elide: Gradle Plugin Convention
 *
 * Applies conventions for building and shipping a published Gradle plugin, from the `build-infra` project
 */
public class ElideGradlePlugin : Plugin<Project> {
  override fun apply(target: Project) {
    // configure environment to build gradle plugins with java and kotlin
    target.pluginManager.apply(BaselineConventions::class.java)
    target.pluginManager.apply(BuildConstants.KnownPlugins.JAVA_GRADLE_PLUGIN)
    target.pluginManager.apply(BuildConstants.KnownPlugins.GRADLE_PLUGIN_PUBLISH)

    val api: Configuration = target.configurations.named("api").get()
    target.dependencies {
      api(gradleApi())
      api("dev.elide.infra:build-infra")
    }

    // configure testing for plugins
    target.pluginManager.apply(ElidePluginTestingConventions::class.java)
  }
}
