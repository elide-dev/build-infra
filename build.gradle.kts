import io.gitlab.arturbosch.detekt.Detekt

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

plugins {
  wrapper
  `build-dashboard`
  `jacoco-report-aggregation`
  `jvm-toolchains`
  `project-reports`
  `test-report-aggregation`

  alias(core.plugins.gradle.checksum)
  alias(core.plugins.gradle.doctor)
  alias(core.plugins.versions)
  alias(libs.plugins.dependencyAnalysis)
  id(core.plugins.kotlin.multiplatform.get().pluginId) apply false
  id("dev.elide.base")
  id("infra.root")
}

description = "Shared conventions and plugins for modern Gradle builds"

buildscript {
  dependencies {
    classpath(libs.guava)
  }

  repositories {
    maven {
      name = "pkgst-maven"
      url = uri("https://maven.pkg.st")
    }
    maven {
      name = "pkgst-gradle"
      url = uri("https://gradle.pkg.st")
    }
  }
}

buildInfra {
  baselines = true
  dependencies.locking.enabled = true
  dependencies.verification.enabled = true
}

private fun Task.taskInAllBuilds(name: String, includeMetabuild: Boolean = false, includeSamples: Boolean = true) {
  dependsOn(gradle.includedBuilds.filter {
    includeSamples || !it.projectDir.path.contains("samples")
  }.map { it.task(":$name") })

  if (includeMetabuild) {
    dependsOn(gradle.includedBuild("build-infra").task(":$name"))
  }
}

// Top-level tasks.

val clean by tasks.registering {
  group = "clean"
  description = "Clean all built targets in all projects"
  taskInAllBuilds("clean")
}
val build by tasks.registering {
  group = "build"
  description = "Build all targets in all projects"
  taskInAllBuilds("build")
}
val test by tasks.registering {
  group = "verification"
  description = "Run all test suites in all projects"
  taskInAllBuilds("test", includeMetabuild = true)
}
val reports by tasks.registering {
  group = "reporting"
  description = "Generate all reports in all projects"
  taskInAllBuilds("reports", includeMetabuild = true)
}
val check by tasks.registering {
  group = "verification"
  description = "Run all checks in all projects"
  dependsOn(tasks.detekt)
  taskInAllBuilds("check", includeMetabuild = true)
  taskInAllBuilds("detekt", includeMetabuild = true, includeSamples = false)
}
val format by tasks.registering {
  group = "formatting"
  description = "Clean-up and format all code"
}
val baselines by tasks.registering {
  group = "verification"
  description = "Scan and set baselines for checks in all projects"
  dependsOn(tasks.detektBaseline)
  taskInAllBuilds("detektBaseline", includeMetabuild = true, includeSamples = false)
}
val detekt by tasks.getting(Detekt::class) {
  group = "verification"
  description = "Run Detekt on all source sets in all projects"
}
