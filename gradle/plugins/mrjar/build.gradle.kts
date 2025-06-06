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
  id("infra.root")
  id("infra.gradle.plugin")
}

description = "Gradle Plugin for producing bytecode-optimized Multi-Release JARs (MRJARs)"

kotlin {
  explicitApi()
}

dependencies {
  api("dev.elide.infra:base")
  api("dev.elide.infra:jpms")
}

gradlePlugin {
  plugins {
    create("mrjar") {
      id = "dev.elide.mrjar"
      implementationClass = "dev.elide.infra.gradle.mrjar.GradleMultiReleaseJarPlugin"
    }
  }
}
