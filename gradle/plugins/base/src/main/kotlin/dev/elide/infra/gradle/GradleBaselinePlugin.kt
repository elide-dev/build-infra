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

import dev.elide.infra.gradle.api.ElideBuild
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.configure

private val projectBaselines: List<Project.(ElideBuild) -> Unit> = listOf(
  { configureArchiveTasks(it) },
  { configureDependencyLocking(it) },
)

/**
 * # Gradle Baseline Plugin
 */
public abstract class GradleBaselinePlugin : Plugin<Project> {
  override fun apply(target: Project) {
    // install configuration extension
    target.extensions.create(BuildConstants.Extensions.INFRA, ElideBuild.ElideBuildDsl::class.java)

    // apply project baselines
    target.extensions.configure<ElideBuild.ElideBuildDsl> {
      // dispatch baselines
      projectBaselines.forEach { it.invoke(target, this@configure) }

      // apply configuration hooks
      target.configurations.all {
        configureDependencyVerification(this@configure)
        filterDisableDependencyLocking(this@configure)
        filterDisableDependencyVerification(this@configure)
      }
    }
  }
}

// Configure archives for better reproducibility.
private fun Project.configureArchiveTasks(conventions: ElideBuild) {
  if (conventions.baselines.get()) listOf(Tar::class, Zip::class, Jar::class).forEach {
    tasks.withType(it.java).configureEach {
      isPreserveFileTimestamps = false
      isReproducibleFileOrder = true
      if (this is Zip) isZip64 = true
    }
  }
}

// Configure dependency locking features.
private fun Project.configureDependencyLocking(conventions: ElideBuild) {
  conventions.dependencies.locking.let { locking ->
    // apply dependency locking
    if (locking.enabled.get()) dependencyLocking {
      lockAllConfigurations()
    }
  }
}

// Apply dependency locking filters.
private fun Configuration.filterDisableDependencyLocking(conventions: ElideBuild) {
  if (conventions.dependencies.locking.ignored.get().any {
    it == this.name || this.name.lowercase().trim().contains(it)
  }) {
    resolutionStrategy.deactivateDependencyLocking()
  }
}

// Configure dependency verification features.
private fun Configuration.configureDependencyVerification(conventions: ElideBuild) {
  conventions.dependencies.verification.let { verification ->
    // apply dependency locking
    if (verification.enabled.get()) resolutionStrategy {
      enableDependencyVerification()
    }
  }
}

// Apply dependency verification filters.
private fun Configuration.filterDisableDependencyVerification(conventions: ElideBuild) {
  if (conventions.dependencies.verification.ignored.get().any {
      it == this.name || this.name.lowercase().trim().contains(it)
    }) {
    resolutionStrategy.disableDependencyVerification()
  }
}
