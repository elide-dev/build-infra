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

pluginManagement {
  includeBuild("../build-infra")
  includeBuild("../libs")
  includeBuild("../platforms")
  includeBuild("../plugins/base")
  includeBuild("../plugins/gha")
  includeBuild("../plugins/graalvm")
  includeBuild("../plugins/jlink")
  includeBuild("../plugins/jmod")
  includeBuild("../plugins/jpms")
  includeBuild("../plugins/mrjar")
}

plugins {
  id("infra.settings")
}

listOf(
  "base",
  "gha",
  "graalvm",
  "jlink",
  "jmod",
  "jpms",
  "mrjar",
).forEach {
  includeBuild("../plugins/$it") {
      dependencySubstitution {
          substitute(module("dev.elide.infra:$it")).using(project(":"))
      }
  }
}

listOf(
  "jmod-kotlin",
  "jmod-library",
  "mrjar-purejava",
).forEach {
  include(":$it")
}

rootProject.name = "samples"
