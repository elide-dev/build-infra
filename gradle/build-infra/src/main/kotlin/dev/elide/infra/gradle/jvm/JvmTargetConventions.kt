@file:Suppress("UNUSED_PARAMETER", "UnusedReceiverParameter")

package dev.elide.infra.gradle.jvm

import dev.elide.infra.gradle.api.ElideBuild
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.process.CommandLineArgumentProvider

// Applies JVM target and task settings.
internal class JvmTargetConventions : Plugin<Project> {
  override fun apply(target: Project) {
    // configure all JAR tasks for sanity
    target.tasks.withType(Jar::class.java) {
      isPreserveFileTimestamps = false
      isReproducibleFileOrder = true
      isZip64 = true
    }

    target.extensions.getByType(ElideBuild.ElideBuildDsl::class.java).let { conventions ->
      // injection of compile-time flags according to conventions
      target.tasks.withType(JavaCompile::class.java).configureEach { configureJavaCompilation(target, conventions) }

      // injection of exec-time flags according to conventions
      target.tasks.withType(JavaExec::class.java).configureEach { configureJavaExec(target, conventions) }
    }
  }
}

private fun MutableList<String>.commonArgs(conventions: ElideBuild.ElideBuildDsl) {
  if (conventions.jvm.preview.get()) add("--enable-preview")
}

private fun MutableList<String>.javacArgs(conventions: ElideBuild.ElideBuildDsl) {
  commonArgs(conventions)
}

private fun MutableList<String>.jvmArgs(conventions: ElideBuild.ElideBuildDsl) {
  commonArgs(conventions)
}

// Configure Java compilation tasks according to JVM build conventions.
private fun JavaCompile.configureJavaCompilation(project: Project, conventions: ElideBuild.ElideBuildDsl) {
  options.compilerArgumentProviders.add(CommandLineArgumentProvider {
    mutableListOf<String>().apply { javacArgs(conventions) }
  })
}

// Configure Java execution tasks according to JVM build conventions.
private fun JavaExec.configureJavaExec(project: Project, conventions: ElideBuild.ElideBuildDsl) {
  jvmArgumentProviders.add(CommandLineArgumentProvider {
    mutableListOf<String>().apply { jvmArgs(conventions) }
  })
}
