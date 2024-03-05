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

package dev.elide.infra.gradle.testing

import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.bnorm.power.PowerAssertGradleExtension
import dev.elide.infra.gradle.BuildConstants
import dev.elide.infra.gradle.baseline.AggregateTargetPlugin
import dev.elide.infra.gradle.baseline.AggregateTargetService
import dev.elide.infra.gradle.baseline.AggregateTargetService.StandardTargetType
import org.gradle.accessors.dm.LibrariesForCore
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.the
import org.gradle.testing.base.TestingExtension

/**
 * # Elide: Build Infra Plugin Testing
 */
public class ElidePluginTestingConventions : Plugin<Project> {
  override fun apply(target: Project) {
    target.pluginManager.apply(ElideBaselineTestingConventions::class.java)
    target.pluginManager.apply(BuildConstants.KnownPlugins.KOTLIN_POWER_ASSERT)

    val libs = target.the<LibrariesForLibs>()
    val core = target.the<LibrariesForCore>()
    val testApi: Configuration = target.configurations.named("testApi").get()
    val testImplementation: Configuration = target.configurations.named("testImplementation").get()
    val testRuntimeOnly: Configuration = target.configurations.named("testRuntimeOnly").get()

    target.dependencies {
      testApi(core.kotlin.test)
      testImplementation(libs.testing.junit.jupiter)
      testImplementation(libs.testing.junit.jupiter.engine)
      testImplementation(libs.testing.junit.jupiter.params)
      testImplementation(core.bundles.asm)
      testImplementation(core.javapoet)
      testImplementation(core.kotlinpoet)
      testImplementation(gradleTestKit())
      testRuntimeOnly(libs.testing.junit.platform.console)
      testApi(group = "dev.elide.infra", name = "build-infra", configuration = "testing")
    }

    target.tasks.named("test", Test::class.java) {
      useJUnitPlatform()
    }

    target.pluginManager.withPlugin(BuildConstants.KnownPlugins.KOTLIN_POWER_ASSERT) {
      target.configure<PowerAssertGradleExtension> {
        functions = powerAssertDefaultFunctions.toList()
      }
    }

    target.extensions.configure<TestLoggerExtension> {
      slowThreshold = 120_000L
    }

    target.plugins.withType(AggregateTargetPlugin::class.java) {
      target.afterEvaluate {
        val suites = the<TestingExtension>().suites.withType(JvmTestSuite::class.java)

        gradle.sharedServices.registrations.named(AggregateTargetPlugin.SERVICE_NAME).get().service.get().apply {
          this as AggregateTargetService
          suites.forEach {
            register(StandardTargetType.JVM_TEST_SUITE, it.name, it)
          }
        }
      }
    }
  }
}
