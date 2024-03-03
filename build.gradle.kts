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
  `build-dashboard`
  `jacoco-report-aggregation`
  `jvm-toolchains`
  `project-reports`
  `test-report-aggregation`

  alias(core.plugins.gradle.checksum)
  alias(core.plugins.gradle.doctor)
  alias(core.plugins.versions)
  id(core.plugins.kotlin.multiplatform.get().pluginId) apply false
  id("dev.elide.base")
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

private fun Task.taskInAllBuilds(name: String) {
  dependsOn(gradle.includedBuilds.map { it.task(":$name") })
}

// Top-level tasks.

val clean by tasks.registering { taskInAllBuilds("clean") }
val test by tasks.registering { taskInAllBuilds("test") }
val check by tasks.registering { taskInAllBuilds("check") }
val build by tasks.registering { taskInAllBuilds("build") }
