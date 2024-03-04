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

package dev.elide.infra.gradle.api

import dev.elide.infra.gradle.BuildConstants
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.jvm.toolchain.*
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.support.serviceOf
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString

/**
 * Resolve the configured Java bytecode target.
 *
 * @param toolchain Toolchain to resolve from
 * @param java Java plugin settings
 * @param conventions Conventions to resolve from
 * @return JVM target
 */
public fun Project.resolveJavaBytecodeTarget(
  toolchain: JavaToolchainSpec,
  java: JavaPluginExtension?,
  conventions: ElideBuild.ElideBuildDsl,
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

/**
 * Resolve the configured Java bytecode minimum.
 *
 * @param conventions Conventions to resolve from
 * @return JVM target, or `null` if no minimum is set
 */
public fun Project.resolveJavaBytecodeMinimum(conventions: ElideBuild.ElideBuildDsl): JvmTarget? {
  // resolve the minimum bytecode level
  return (findProperty(BuildConstants.Properties.JVM_MINIMUM) as? String)
    ?.ifBlank { null }
    ?.toIntOrNull()
    ?.let { JvmTarget.fromTarget(it.toString()) }
    ?: conventions.jvm.minimum.orNull
}

// GraalVM home path.
private val graalvmHome: () -> Path? = {
  System.getenv("GRAALVM_HOME")
    ?.ifBlank { null }
    ?.let { Path(it).toAbsolutePath() }
}

/**
 * ## JDK File
 *
 * Resolve a file path from the JAVA_HOME used as the Java toolchain.
 *
 * @param first First segment of the file path to resolve
 * @param segments Further segments of the file path to resolve
 * @return Absolute path to the requested file
 */
public fun Project.javaHomeFile(first: String, vararg segments: String): String {
  val javaToolchainService = project.serviceOf<JavaToolchainService>()
  val toolchain = project.extensions.getByType<JavaPluginExtension>().toolchain
  val defaultLauncher = javaToolchainService.launcherFor(toolchain)
  val javaHomeBase = defaultLauncher.get().executablePath.asFile.parentFile.parentFile.toPath()
  return javaHomeBase.resolve(Path(first, *segments)).toString()
}

/**
 * ## GraalVM File
 *
 * Resolve a file path from the `GRAALVM_HOME`.
 *
 * @param first First segment of the file path to resolve
 * @param segments Further segments of the file path to resolve
 * @return Absolute path to the requested file
 */
public fun graalvmFile(first: String, vararg segments: String): String? =
  graalvmHome.invoke()?.resolve(Path(first, *segments))?.toString()

/**
 * ## Project-Relative Paths
 *
 * Transform a file path to be project-relative. This utility will transform the provided [path] by trimming it based on
 * the current [Project] path.
 *
 * Gradle build caching works best when relative paths are used. Most build tasks are run from the project directory,
 * which includes nesting in a multi-module build.
 *
 * Use example:
 * ```kotlin
 * val somePath = Path("/some/long/absolute/path/modules/a/build/libs/a.jar")
 * projectRelative(somePath)  // returns `modules/a/build/libs/a.jar`
 * ```
 *
 * @param path Path to trim
 * @return String path, trimmed to be project-relative
 */
public fun Project.projectRelative(path: Path): String {
  val cwd = project.layout.projectDirectory.asFile.toPath()
  require(path.startsWith(cwd)) { "Cannot trim project-relative path which is not in project: $path ($cwd)" }
  return path
    .toAbsolutePath()
    .normalize()
    .pathString
    .drop(cwd.toAbsolutePath().normalize().pathString.length)
    .drop(1)  // initial slash
}

/**
 * ## Java Toolchain (Current)
 *
 * Resolve the current Java toolchain.
 *
 * Use example:
 * ```kotlin
 * val current = javaToolchainAt()
 * ```
 *
 * @return Java Toolchain specification
 */
public fun Project.javaToolchain(): JavaToolchainSpec {
  return extensions.getByType<JavaPluginExtension>().toolchain
}

/**
 * ## Java Toolchain
 *
 * Resolve a Java toolchain for the provided language [version].
 *
 * Use example:
 * ```kotlin
 * val toolchain = javaToolchainAt(/* `21` */)
 * ```
 *
 * @param version Desired version; optional
 * @return Java Toolchain specification
 */
public fun Project.javaToolchainAt(version: Int? = null): JavaToolchainSpec {
  val toolchain = extensions.getByType<JavaPluginExtension>().toolchain
  return when {
    // if no version is specified, or if the specified version is runnable/compilable by the current toolchain, use it
    version != null && toolchain.languageVersion.get().canCompileOrRun(version) -> toolchain

    // otherwise, we will need to request a specific toolchain launcher version
    else -> error("Assigned toolchain '$toolchain' cannot support requested JVM version $version")
  }
}

// Select a toolchain at the optional `version` (or active toolchain), then call `andThen` on it.
private fun <V> Project.selectToolchainAt(
  version: JvmTarget? = null,
  andThen: JavaToolchainSpec.() -> Provider<V>,
): Provider<V> = selectToolchainAt(version?.target?.toIntOrNull(), andThen)

// Select a toolchain at the optional `version` (or active toolchain), then call `andThen` on it.
private fun <V> Project.selectToolchainAt(
  version: Int? = null,
  andThen: JavaToolchainSpec.() -> Provider<V>,
): Provider<V> = extensions.getByType<JavaPluginExtension>().toolchain.let { toolchain ->
  val langVersion = (
    toolchain.languageVersion.orNull ?:
    JavaLanguageVersion.of(Runtime.version().version().first())
  )

  when {
    // if no version is specified, or if the specified version is runnable/compilable by the current toolchain, use it
    version == null || langVersion.canCompileOrRun(version) -> andThen.invoke(toolchain)

    // otherwise, we will need to request a specific toolchain launcher version
    else -> andThen.invoke(object: JavaToolchainSpec {
      override fun getDisplayName(): String = "JVM $version, Vendor ${toolchain.vendor.get()}"
      override fun getLanguageVersion(): Property<JavaLanguageVersion> =
        objects.property(JavaLanguageVersion::class.java).apply {
          set(JavaLanguageVersion.of(version))
        }

      override fun getVendor(): Property<JvmVendorSpec> =
        objects.property(JvmVendorSpec::class.java).apply {
          if (toolchain.vendor.isPresent) set(toolchain.vendor)
        }

      override fun getImplementation(): Property<JvmImplementation> =
        objects.property(JvmImplementation::class.java).apply {
          if (toolchain.vendor.isPresent) set(toolchain.implementation)
        }
    })
  }
}

/**
 * ## Java Compiler
 *
 * Resolve a Java compiler for the provided language [version].
 *
 * Use example:
 * ```kotlin
 * val compiler = javaCompilerAt(/* `21` */)
 * ```
 *
 * @param version Desired version; optional
 * @param service Pre-resolved toolchain service to use
 * @return Java compiler provider
 */
public fun Project.javaCompilerAt(
  version: JvmTarget,
  service: JavaToolchainService? = null,
): Provider<JavaCompiler> = (service ?: serviceOf<JavaToolchainService>()).let {
  selectToolchainAt(version) { it.compilerFor(this) }
}

/**
 * ## Java Launcher
 *
 * Resolve a Java launcher for the provided language [version].
 *
 * Use example:
 * ```kotlin
 * val launcher = javaLauncherAt(/* `21` */)
 * ```
 *
 * @param version Desired version; optional
 * @param service Pre-resolved toolchain service to use
 * @return Java launcher provider
 */
public fun Project.javaLauncherAt(
  version: JvmTarget,
  service: JavaToolchainService? = null,
): Provider<JavaLauncher> = (service ?: serviceOf<JavaToolchainService>()).let {
  selectToolchainAt(version) { it.launcherFor(this) }
}
