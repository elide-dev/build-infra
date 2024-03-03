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

import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.theme.ThemeType
import dev.elide.infra.gradle.BuildConstants
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * # Elide: Baseline Testing Conventions
 */
public class ElideBaselineTestingConventions : Plugin<Project> {
  override fun apply(target: Project) {
    target.pluginManager.apply(BuildConstants.KnownPlugins.TEST_LOGGER)
    target.pluginManager.withPlugin(BuildConstants.KnownPlugins.TEST_LOGGER) {
      target.extensions.configure<TestLoggerExtension> {
        configureTestLogger(target)
      }
    }
  }
}

// Configure the test-logger extension.
private fun TestLoggerExtension.configureTestLogger(project: Project) {
  val shouldShowExceptions = (
    (project.findProperty(BuildConstants.Properties.TEST_EXCEPTIONS) as? String)?.ifBlank { null } ?:
    System.getenv(BuildConstants.Environment.TEST_EXCEPTIONS)?.ifBlank { null }
  )?.toBooleanStrictOrNull() ?: false

  val shouldShowLogs = (
    (project.findProperty(BuildConstants.Properties.TEST_LOGS) as? String)?.ifBlank { null } ?:
    System.getenv(BuildConstants.Environment.TEST_LOGS)?.ifBlank { null }
  )?.toBooleanStrictOrNull() ?: false

  val testNameModeOverride = (
    (project.findProperty(BuildConstants.Properties.TEST_LOGS) as? String)?.ifBlank { null } ?:
    System.getenv(BuildConstants.Environment.TEST_LOGS)?.ifBlank { null }
  )

  theme = ThemeType.MOCHA_PARALLEL
  showPassed = true
  showFailed = true
  showSkipped = true
  slowThreshold = 30_000L
  isShowCauses = shouldShowExceptions
  isShowSimpleNames = testNameModeOverride != "full"
  showStackTraces = shouldShowExceptions
  showExceptions = shouldShowExceptions
  showStandardStreams = shouldShowLogs
}
