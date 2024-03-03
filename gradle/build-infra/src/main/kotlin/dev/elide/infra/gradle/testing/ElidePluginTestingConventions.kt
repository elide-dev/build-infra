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

package dev.elide.infra.gradle.testing

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.the

/**
 * # Elide: Build Infra Plugin Testing
 */
public class ElidePluginTestingConventions : Plugin<Project> {
  override fun apply(target: Project) {
    target.pluginManager.apply(ElideBaselineTestingConventions::class.java)
    val libs = target.the<LibrariesForLibs>()
    val testApi: Configuration = target.configurations.named("testApi").get()
    val testImplementation: Configuration = target.configurations.named("testImplementation").get()
    val testRuntimeOnly: Configuration = target.configurations.named("testRuntimeOnly").get()

    target.dependencies {
      testImplementation(gradleTestKit())
      testImplementation(libs.testing.junit.jupiter)
      testImplementation(libs.testing.junit.jupiter.engine)
      testImplementation(libs.testing.junit.jupiter.params)
      testRuntimeOnly(libs.testing.junit.platform.console)
      testApi(group = "dev.elide.infra", name = "build-infra", configuration = "testing")
    }

    target.tasks.named("test", Test::class.java) {
      useJUnitPlatform()
    }
  }
}
