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

@file:Suppress("UnstableApiUsage")

package dev.elide.infra.gradle

import dev.elide.infra.gradle.baseline.AggregateTargetPlugin
import dev.elide.infra.gradle.baseline.AggregateTargetService
import dev.elide.infra.gradle.checks.DetektConvention
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.jvm.JvmTestSuite

/**
 * # Convention: Root Build
 *
 * Configures dependencies and settings which are unique to the root build; provided at `elide.root`.
 */
public class RootBuildConvention : Plugin<Project> {
  override fun apply(target: Project) {
    target.pluginManager.apply(BaselinePlugin::class.java)
    target.pluginManager.apply(DetektConvention::class.java)
    target.pluginManager.apply(BuildConstants.KnownPlugins.TEST_REPORT_AGGREGATION)
    target.pluginManager.apply(BuildConstants.KnownPlugins.JACOCO_REPORT_AGGREGATION)
    target.pluginManager.apply(BuildConstants.KnownPlugins.BUILD_DASHBOARD)
    target.pluginManager.apply(BuildConstants.KnownPlugins.PROJECT_REPORTS)

    target.gradle.projectsEvaluated {
      // after all projects are evaluated, trigger a hook to configure aggregated services
      gradle.sharedServices.registrations.named(AggregateTargetPlugin.SERVICE_NAME).get().service.get().apply {
        this as AggregateTargetService
        val allTestSuites = resolve<JvmTestSuite>(AggregateTargetService.StandardTargetType.JVM_TEST_SUITE).toList()
        if (allTestSuites.isNotEmpty()) {
          allTestSuites.forEach {
            //

            target.dependencies.apply {

            }
          }
        }
      }
    }
  }
}
