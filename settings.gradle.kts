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

pluginManagement {
  includeBuild("gradle/build-infra")

  // see `gradle.properties` for list of plugins
  (extra["build-infra.plugins"] as String).split(",").forEach {
    includeBuild("gradle/plugins/$it")
  }
}

plugins {
  `jvm-toolchain-management`
  id("infra.settings")
}

// Plugins provided by this project; these live under `gradle/plugins/<name>`.
val allPlugins = (extra["build-infra.plugins"] as String).split(",")

// Add libraries, which supplement plug-ins with catalogs and logic.
includeBuild("gradle/libs")

// Add platforms, which provide Gradle JVM platform target definitions.
includeBuild("gradle/platforms")

// Add samples, which test the embedded plugins.
includeBuild("gradle/samples")

// Load each plug-in project.
allPlugins.forEach {
  includeBuild("gradle/plugins/$it") {
    dependencySubstitution {
      // map `dev.elide.infra:<plugin>` → `:<plugin`
      substitute(module("dev.elide.infra:$it")).using(project(":"))
    }
  }
}

// Map the outer `dev.elide.infra:build-infra` project → `build-infra`.
includeBuild("gradle/build-infra") {
  dependencySubstitution {
    listOf("dev.elide.infra:build-infra").forEach {
      substitute(module(it)).using(project(":"))
    }
  }
}

rootProject.name = "build-commons"
