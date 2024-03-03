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

@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package dev.elide.infra.gradle.kotlin

import dev.elide.infra.gradle.BuildConstants
import dev.elide.infra.gradle.api.ElideBuild
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

/**
 * # Kotlin: JVM Conventions
 */
public class KotlinJvmConventions : Plugin<Project> {
  override fun apply(target: Project) {
    target.extensions.getByType(ElideBuild.ElideBuildDsl::class.java).let { conventions ->
      target.extensions.getByType(KotlinMultiplatformExtension::class.java).apply {
        applyDefaultHierarchyTemplate()

        jvm {
          withJava()
        }

        sourceSets.jvmMain {
          dependsOn(sourceSets.commonMain.get())
        }
        sourceSets.jvmTest {
          dependsOn(sourceSets.commonTest.get())
        }

        val apiTarget = (target.findProperty(BuildConstants.Properties.KOTLIN_API) as? String)
          ?.ifBlank { null }
          ?.let { KotlinVersion.fromVersion(it) }
          ?: conventions.kotlin.api.get()
        val languageTarget = (target.findProperty(BuildConstants.Properties.KOTLIN_LANGUAGE) as? String)
          ?.ifBlank { null }
          ?.let { KotlinVersion.fromVersion(it) }
          ?: conventions.kotlin.language.get()

        compilerOptions {
          optIn.set(conventions.kotlin.optIns)
          apiVersion.set(apiTarget)
          languageVersion.set(languageTarget)
        }
      }
    }
  }
}
