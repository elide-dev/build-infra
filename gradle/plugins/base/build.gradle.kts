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
  `kotlin-dsl`
  `version-catalog`
  `maven-publish`

  id("infra.root")
  id("infra.library")
  id("infra.gradle.plugin")
}

description = "Gradle Plugin for establishing healthy project baseline settings"

kotlin {
  explicitApi()
}

gradlePlugin {
  plugins {
    create("base") {
      id = "dev.elide.base"
      implementationClass = "dev.elide.infra.gradle.GradleBaselinePlugin"
    }
  }
}
