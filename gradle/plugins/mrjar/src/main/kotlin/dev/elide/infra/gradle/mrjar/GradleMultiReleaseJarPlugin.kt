package dev.elide.infra.gradle.mrjar

import dev.elide.infra.gradle.jpms.GradleJpmsPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * # Gradle MRJAR Plugin
 */
public abstract class GradleMultiReleaseJarPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    // always apply the baseline jpms plugin
    if (!target.plugins.hasPlugin(GradleJpmsPlugin::class.java))
      target.pluginManager.apply(GradleJpmsPlugin::class.java)

    // determine current target bytecode version
    val bytecodeTarget = target.resolveJavaBytecodeTarget()
    val bytecodeMinimum = target.resolveJavaBytecodeMinimum()
    val modularity = target.detectModularProject()

    when {
      // if the bytecode target and minimum are the same, there is no need for a multi-release JAR; the artifact will
      // simply embed compiled classes at the expected target level.
      bytecodeTarget == bytecodeMinimum -> target.logger.info(
        "No multi-release JAR is needed: bytecode target is the same as bytecode minimum."
      )

      // otherwise, there is *some* difference in the bytecode minimum, to be accounted for in a multi-release JAR, but
      // we only need to position the `module-info` in `META-INF/...` if we need to support anything before Java 9. This
      // condition only applies, though, if we're building a modular project.
      bytecodeMinimum < JvmTarget.JVM_9 && modularity != null -> {
        target.logger.info("MR JAR crosses JVM8/JVM9 boundary; building modules in MR JAR")
      }

      // at JVM9+ bytecode minimum, with modularity active, we can build a regular `module-info.class` in the root of
      // the JAR, and proceed to build bytecode for each included target.
      modularity != null -> {}

      // otherwise, we have no modularity needs and can simply build the supported bytecode tiers.
      else -> {}
    }
  }
}

// Return a pair of the project's JPMS module name and application name, as applicable.
private fun Project.detectModularProject(): Pair<String, String?>? {
  TODO("not yet implemented")
}

// Resolve the configured Java bytecode target.
private fun Project.resolveJavaBytecodeTarget(): JvmTarget {
  TODO("not yet implemented")
}

// Resolve the configured Java bytecode minimum.
private fun Project.resolveJavaBytecodeMinimum(): JvmTarget {
  TODO("not yet implemented")
}
