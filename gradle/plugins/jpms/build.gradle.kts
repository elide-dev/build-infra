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

description = "Gradle Plugin for integration with the Java Platform Module System (JPMS)"

kotlin {
  explicitApi()
}

dependencies {
  api(core.plugin.kotlin.multiplatform)
  api("dev.elide.infra:base")
}

gradlePlugin {
  plugins {
    create("jpms") {
      id = "dev.elide.jpms"
      implementationClass = "dev.elide.infra.gradle.jpms.GradleJpmsPlugin"
    }
  }
}
