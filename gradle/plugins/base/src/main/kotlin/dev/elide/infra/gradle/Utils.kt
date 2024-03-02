package dev.elide.infra.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.support.serviceOf
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString

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
