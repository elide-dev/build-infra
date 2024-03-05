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
import dev.elide.infra.gradle.api.toJavaLanguageVersion
import dev.elide.infra.gradle.api.toJavaVersion
import dev.elide.infra.gradle.baseline.BaselineConventions
import dev.elide.infra.gradle.checks.DetektConvention
import dev.elide.infra.gradle.testing.ElidePluginTestingConventions
import kotlinx.kover.gradle.plugin.KoverGradlePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.extra
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import kotlin.reflect.KClass

// Settings which apply to all plugins
private const val kotlinStrict = false
private val pluginJvmTarget = JvmTarget.JVM_11
private val pluginJvmToolchain = JvmTarget.JVM_21
private val pluginKotlinTarget = KotlinVersion.KOTLIN_1_9
private val kotlincArgs = listOf<String>()
private val kotlincOptins = listOf<String>()
private val javacArgs = listOf<String>()

// Plugins to apply to the root build of plugin projects.
private val pluginPlugins: List<KClass<out Plugin<Project>>> = listOf(
  KoverGradlePlugin::class,
  DetektConvention::class,
  DokkaPlugin::class,
  ElidePluginTestingConventions::class,
)

/**
 * # Elide: Gradle Plugin Convention
 *
 * Applies conventions for building and shipping a published Gradle plugin, from the `build-infra` project
 */
public class ElideGradlePlugin : Plugin<Project> {
  override fun apply(target: Project) {
    // configure target range
    target.extra.set("conventions.jvm.minimum", pluginJvmTarget.target)
    target.extra.set("conventions.jvm.target", pluginJvmToolchain.target)
    target.extra.set("conventions.jvm.toolchain", pluginJvmToolchain.target)

    // configure environment to build gradle plugins with java and kotlin
    target.pluginManager.apply(BuildConstants.KnownPlugins.BASE)
    target.pluginManager.apply(RootBuildConvention::class.java)
    target.pluginManager.apply(BuildConstants.KnownPlugins.JAVA_GRADLE_PLUGIN)
    target.pluginManager.apply(BuildConstants.KnownPlugins.GRADLE_PLUGIN_PUBLISH)

    val api: Configuration = target.configurations.named("api").get()
    target.dependencies {
      api(gradleApi())
      api("dev.elide.infra:build-infra")
    }

    target.plugins.withType(BaselineConventions::class.java) {
      target.extensions.configure<ElideBuild.ElideBuildDsl> {
        jvm.toolchain.set(pluginJvmToolchain.toJavaVersion())
        jvm.target.set(pluginJvmToolchain)  // use top-most toolchain version
        jvm.minimum.set(pluginJvmTarget)  // minimum target version
      }

      // configure compilers
      target.configureJavaCompiler()
      target.configureKotlinCompiler()

      // add all plugins
      target.afterEvaluate {
        pluginPlugins.forEach {
          target.pluginManager.apply(it.java)
        }
      }
    }
  }
}

// Configure the Kotlin compiler for this plug-in.
private fun Project.configureJavaCompiler() {
  extensions.configure<JavaPluginExtension> {
    pluginJvmToolchain.toJavaVersion().let {
      sourceCompatibility = it
      targetCompatibility = it
    }

    toolchain {
      languageVersion.set(pluginJvmToolchain.toJavaLanguageVersion())
    }
  }

  tasks.named("compileJava", JavaCompile::class.java) {
    pluginJvmToolchain.toJavaVersion().let {
      sourceCompatibility = it.majorVersion
      targetCompatibility = it.majorVersion
    }

    options.compilerArgumentProviders.add(CommandLineArgumentProvider {
      javacArgs
    })
  }
}

// Configure the Kotlin compiler for this plug-in.
private fun Project.configureKotlinCompiler() {
  tasks.withType(KotlinJvmCompile::class.java).configureEach {
    jvmTargetValidationMode.set(JvmTargetValidationMode.ERROR)

    compilerOptions {
      javaParameters.set(true)
      apiVersion.set(pluginKotlinTarget)
      languageVersion.set(pluginKotlinTarget)
      allWarningsAsErrors.set(kotlinStrict)
      progressiveMode.set(kotlinStrict)
      jvmTarget.set(pluginJvmToolchain)
      freeCompilerArgs.addAll(kotlincArgs)
      optIn.addAll(kotlincOptins)
    }
  }
}
