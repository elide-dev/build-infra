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

package dev.elide.infra.gradle.mrjar

import dev.elide.infra.gradle.BuildConstants
import dev.elide.infra.gradle.api.ElideBuild.ElideBuildDsl
import dev.elide.infra.gradle.jpms.GradleJpmsPlugin
import dev.elide.infra.gradle.jpms.Java9Modularity
import dev.elide.infra.gradle.jpms.Java9Modularity.configureModularity
import dev.elide.infra.gradle.jpms.Java9Modularity.configureMultiReleaseJar
import dev.elide.infra.gradle.jpms.ModularityConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JavaToolchainSpec
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import java.nio.charset.StandardCharsets

// Regex for detecting `module-info` module names.
private val moduleNameRegex = Regex(
  "(?s)^module ([a-zA-Z1-9.]{1,99}) [{].*$.*",
  setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL),
)

/**
 * # Gradle MRJAR Plugin
 *
 * This plugin implements support for building Multi-Release JARs, also known as "MRJARs." MRJARs are defined by JSR 376
 * as part of the Java Platform Module System (JPMS). MRJARs were conceived as a way to ship Java 9+-compatible modules
 * while retaining support for pre-JDK9 versions, like, JDK8.
 *
 * Normally, MRJARs contain just the class overrides they need for each version of the JVM. This plugin does things a
 * bit differently, by building at each bytecode tier **in full**, and placing the entire output in the JAR.
 *
 * In effect, this creates a "fat" JAR from the perspective of JVM bytecode. Bytecode is loaded at the highest supported
 * tier by the effective runtime, with no degradation in target compatibility. Newer bytecode output sets can use modern
 * Java features for the version they target -- safely -- without compromising other source sets.
 *
 * ### Usage
 *
 * ```kotlin
 * plugins {
 *   id("dev.elide.mrjar")
 * }
 *
 * mrjar {
 *   // See configuration section.
 * }
 * ```
 *
 * ## Baselines
 *
 * This plug-in applies baseline configurations to the target Gradle project, including better reproducibility for
 * archives, and a handful of other sensible defaults. These can be disabled with:
 *
 * ```kotlin
 * infra {
 *   baselines = false
 * }
 * ```
 *
 * ## Limitations
 *
 * JARs produced in this manner are meant largely for library authors who wish to ship artifacts that self-optimize for
 * the runtime their consuming user chooses. One major restriction for MRJARs is that they have no reasonable way to
 * declare different dependency trees for each JVM target.
 *
 * Thus, libraries built this way should ideally have very small dependency graphs, or dependency graphs which are
 * safely usable on each targeted JVM version.
 *
 * ## How it Works
 *
 * Firstly, a **minimum** and **maximum** JVM target version are resolved; if the two are equal, this plugin is inert.
 * Then, a range of eligible target JVM versions are generated, with "ignorable" versions dropped (intermediate non-LTS
 * versions like Java 10 are skipped by default, unless included explicitly).
 *
 * Phantom source sets are created for each targeted JVM version. Within these source sets, which are optional, the
 * developer can include sources which target that specific runtime version, and which override. For example, given the
 * following project structure:
 *
 * ```
 * project/
 * ├─ src/
 * │  ├─ main/
 * │  │  ├─ java/
 * │  │  │  ├─ module-info.java
 * │  │  │  ├─ ...
 * │  │  ├─ kotlin/
 * │  │  │  ├─ ...
 * │  │  ├─ jvm11/
 * │  │  │  ├─ .../
 * │  │  ├─ jvm17/
 * │  │  │  ├─ .../
 * ```
 *
 * And the settings:
 * - **JVM Minimum:** JVM 11
 * - **JVM Target:** JVM 21
 *
 * The following behaviors would be configured:
 * - MRJAR roots for JVM versions 11, 17, and 21, with bytecode built at those tiers in full
 * - Overrides applied from each source set, with earlier source sets applying in a cascading fashion
 * - Java modularity at all levels of the JAR, starting at the root (read on for more info about JPMS integration)
 *
 * ### Source Sets & Compile Tasks
 *
 * In each `jvm*` source set, Java and Kotlin sources can be mixed, as long as the Kotlin plug-in is included and a JVM
 * target is enabled. Kotlin's JVM codegen target is kept in sync for each source set. The plug-in will clone each
 * source set's compile tasks and configure them for the desired target JVM bytecode version.
 *
 * For each source set, a Java compilation and Kotlin compilation task are created, as applicable. These tasks show up
 * in `./gradlew tasks` and can be run independently for diagnosis and testing.
 *
 * ### JPMS Integration
 *
 * This plugin relies on Gradle's built-in JPMS functionality, and on the Build Infra JPMS Plugin. The base JPMS plugin
 * applies configurations and other settings dedicated to JPMS support.
 *
 * **If you are building a non-modular JVM application,** you don't really need to read this section. Your MRJAR is
 * built with classes as you'd expect, with all the above docs applying, except with no `module-info.java` present.
 *
 * **If you are building a modular JVM application or library,** you should include a `module-info.java`. Don't use
 * automatic modules (please). Your `module-info.java` is built according to the following rules:
 *
 * - If JDK8 is included in your target range, the `module-info.class` is placed within `META-INF/versions/9`
 * - If JDK8 is **not** included in your target range, the `module-info.class` is placed at the root of the JAR
 * - If you provide `module-info.java` declarations specialized for JVM tiers, they are included at that tier
 *
 * Note that Java module declarations interact in interesting ways with MRJARs when multiple module descriptors are
 * included within the JAR. According to spec, this is legal, so long as each module descriptor **maintains an identical
 * public API surface**. Module descriptors are allowed to modify or enhance their implementation module path. Adding
 * `exports` and `uses` is allowed. For more information, see the JPMS and `jmod` docs.
 *
 * @see GradleJpmsPlugin base JPMS settings and configurations
 * @see Java9Modularity Modularity tools for Kotlin and Java builds
 */
public abstract class GradleMultiReleaseJarPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    // always apply the baseline jpms plugin
    if (!target.plugins.hasPlugin(GradleJpmsPlugin::class.java))
      target.pluginManager.apply(GradleJpmsPlugin::class.java)

    // resolve the modulepath configuration, added by the JPMS plugin
    target.pluginManager.withPlugin(BuildConstants.KnownPlugins.INFRA_JPMS) {
      target.afterEvaluate {
        configureModularityPlugin()
      }
    }
  }
}

// Configure modularity plugin features.
private fun Project.configureModularityPlugin() {
  // determine current target bytecode version
  val java = requireNotNull(extensions.findByType<JavaPluginExtension>()) {
    "Failed to locate Java extension, which is required to call `configureModularity`. Please add the `java` plugin."
  }
  val kotlin = extensions.findByType<KotlinProjectExtension>()

  // resolve toolchain service
  val toolchain = project.extensions.getByType<JavaPluginExtension>().toolchain

  val conventions = extensions.getByType(ElideBuildDsl::class.java)
  val bytecodeTarget = resolveJavaBytecodeTarget(toolchain, java, conventions)
  val bytecodeMinimum = resolveJavaBytecodeMinimum(conventions) ?: bytecodeTarget
  val (modular, moduleName) = determineModularity(java, kotlin)
  val modulepath = requireNotNull(configurations.named(BuildConstants.Configurations.MODULEPATH).get()) {
    "Failed to resolve `modulepath` configuration; was the JPMS plugin applied?"
  }

  when {
    // if the bytecode target and minimum are the same, there is no need for a multi-release JAR; the artifact will
    // simply embed compiled classes at the expected target level.
    bytecodeTarget == bytecodeMinimum -> logger.info(
      "No multi-release JAR is needed: bytecode target is the same as bytecode minimum."
    )

    // try to detect if we are dealing with a modular project; this is accomplished by sniffing the `mainModule`
    // attribute of an application extension instance, if available. otherwise, the base source set is searched for
    // a `module-info.java` file.
    !modular -> configureMultiReleaseJar(java, kotlin, ModularityConfig.forRange(bytecodeMinimum, bytecodeTarget))

    // otherwise, there is *some* difference in the bytecode minimum, to be accounted for in a multi-release JAR,
    // but we only need to position the `module-info` in `META-INF/...` if we need to support anything before
    // Java 9. this condition only applies, though, if we're building a modular project.
    bytecodeMinimum < JvmTarget.JVM_9 -> {
      logger.info("MR JAR crosses JVM8/JVM9 boundary; building modules in MR JAR")
      configureModularity(
        java,
        kotlin,
        modulepath,
        moduleName,
        ModularityConfig.forRange(JvmTarget.JVM_1_8, bytecodeTarget).also {
          require(!it.preferModern) {
            "Error: Cannot build multi-release JARs in `preferModern` mode when supporting JDK 8."
          }
        },
      )
    }

    // otherwise, we have no modularity needs and can simply build the supported bytecode tiers.
    else -> configureModularity(
      java,
      kotlin,
      modulepath,
      moduleName,
      ModularityConfig.forRange(bytecodeMinimum, bytecodeTarget),
    )
  }
}

// Determine whether this project defines a modular JVM target.
private fun Project.determineModularity(
  java: JavaPluginExtension,
  kotlin: KotlinProjectExtension?,
): Pair<Boolean, String?> {
  val app = extensions.findByType(JavaApplication::class.java)
  return if (app != null && app.mainModule.isPresent && app.mainModule.get().isNotBlank()) {
    // fast path: we got lucky, and this is an entrypoint module, with the main module declared. no need to search.
    true to app.mainModule.get()
  } else {
    val javaModuleInJava = java.sourceSets.firstNotNullOfOrNull {
      it.java.find { file -> file.name == "module-info.java" }
    }
    val javaModuleInKotlin = kotlin?.sourceSets?.firstNotNullOfOrNull {
      it.kotlin.find { file -> file.name == "module-info.java" }
    }
    require(javaModuleInJava == null || javaModuleInKotlin == null) {
      "Cannot define two `module-info.java` files in the same Kotlin/Java project '${this.name}'"
    }

    val target = javaModuleInJava ?: javaModuleInKotlin
    if (target == null) {
      // the project is not modular, at least within any of our non-generated source sets
      false to null
    } else {
      val moduleInfo = target.inputStream().reader(StandardCharsets.UTF_8).use {
        it.readText()
      }

      // try to parse the module name
      val matches = moduleNameRegex.matchEntire(moduleInfo)
      require(matches != null && matches.groups.size > 1) {
        "Failed to detect `module <module name> {` at path '${target.path}'; is this a valid module descriptor?"
      }
      val moduleName = requireNotNull(matches.groups[1]).value
      true to moduleName
    }
  }
}

// Resolve the configured Java bytecode target.
private fun Project.resolveJavaBytecodeTarget(
  toolchain: JavaToolchainSpec,
  java: JavaPluginExtension?,
  conventions: ElideBuildDsl,
): JvmTarget {
  // regular java target compatibility
  val javaTarget = java?.targetCompatibility?.majorVersion?.let { JvmTarget.fromTarget(it) }

  // resolve the maximum bytecode level
  val target = (findProperty(BuildConstants.Properties.JVM_TARGET) as? String)
    ?.ifBlank { null }
    ?.toIntOrNull()
    ?.let { JvmTarget.fromTarget(it.toString()) }
    ?: conventions.jvm.target.orNull
    ?: javaTarget?.let { JvmTarget.fromTarget(it.target) }
    ?: toolchain.languageVersion.orNull?.let { JvmTarget.fromTarget(it.asInt().toString()) }
    ?: Runtime.version().version().first().let { JvmTarget.fromTarget(it.toString()) }

  // make sure java target aligns
  if (javaTarget != null) require(target == javaTarget) {
    "Please align JVM target between `java.targetCompatiblity` and Build Infra. Got: " +
      "'$target' for Build Infra, '$javaTarget' for the Java plugin."
  }
  return target
}

// Resolve the configured Java bytecode minimum.
private fun Project.resolveJavaBytecodeMinimum(conventions: ElideBuildDsl): JvmTarget? {
  // resolve the minimum bytecode level
  return (findProperty(BuildConstants.Properties.JVM_MINIMUM) as? String)
    ?.ifBlank { null }
    ?.toIntOrNull()
    ?.let { JvmTarget.fromTarget(it.toString()) }
    ?: conventions.jvm.minimum.orNull
}
