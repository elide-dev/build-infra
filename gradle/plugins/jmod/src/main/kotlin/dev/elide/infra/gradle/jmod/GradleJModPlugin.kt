package dev.elide.infra.gradle.jmod

import dev.elide.infra.gradle.javaHomeFile
import dev.elide.infra.gradle.jpms.GradleJpmsPlugin
import dev.elide.infra.gradle.projectRelative
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.named
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/**
 * # Gradle JMod Plugin
 *
 * Provides support for building `jmod` artifacts from Gradle projects; when added to a Java or Kotlin project, a task
 * `jmod` is created, which, when run, builds a `jmod` artifact at `build/libs/<module>.jmod`.
 *
 * To build a Java Module artifact, a project must be modular; in other words, it must provide a `module-info.java`
 * definition. JMod artifacts support a lot more than JARs: header files, native linkage, and man files, to name a few.
 * JMod artifacts are also compatible with `jlink`, even with use of classpath dependencies.
 *
 * ## Baselines
 *
 * This plug-in will also apply the Baselines and JPMS plug-ins. These are pre-requisites for `jmod` builds, and provide
 * the following functionality:
 *
 * - Configuration for managing module dependencies at `modulepath`
 * - Utilities for project paths and JDK asset resolution
 *
 * ## Using the Plugin
 *
 * In your `build.gradle.kts`:
 * ```kotlin
 * plugins {
 *   `java`
 *   id("dev.elide.jmod")
 * }
 *
 * // then, later: ./gradlew jmod
 * ```
 */
public abstract class GradleJModPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    // always apply the JPMS plugin
    if (!target.plugins.hasPlugin(GradleJpmsPlugin::class.java))
      target.pluginManager.apply(GradleJpmsPlugin::class.java)

    // --- Java: Module build task.
    target.tasks.register("jmod", Exec::class.java) {
      val modulepath = target.configurations.named("modulepath").get()
      val tasks = project.tasks
      val jmodOut = project.layout.buildDirectory.file("libs/${project.name}.jmod").get().asFile
      val jmodOutPath = project.projectRelative(jmodOut.toPath())
      val application = project.extensions.findByType(JavaApplication::class.java)

      val javac = tasks.named("compileJava", JavaCompile::class).get()
      val kotlinc = tasks.findByName("compileKotlinJvm")
      val jar = when (val jvmJar = tasks.findByName("jvmJar")) {
        null -> tasks.named("jar", Jar::class).get()
        else -> jvmJar as Jar
      }

      // jmod depends on the outputs of all jvm builds in this module.
      listOfNotNull(
        javac,
        jar,
        kotlinc,
        tasks.findByName("classes"),
        tasks.findByName("jvmMainClasses"),
      ).let {
        dependsOn(it)
        mustRunAfter(it)
      }

      // we will be running jmod today; it's part of the jdk, and we will be outputting a jmod file.
      executable(project.javaHomeFile("bin", "jmod"))
      outputs.file(jmodOut)

      // output classes from java and kotlin compile jobs are inputs
      val outputClasses = listOfNotNull(
        javac.destinationDirectory,
        kotlinc?.let { tasks.named("compileKotlinJvm", KotlinJvmCompile::class).get().destinationDirectory },
      )

      // the JAR is an input to force Gradle to write outputs, including `module-info.class`. the `module-info.class` file
      // is always required for a jmod build.
      inputs.files(outputClasses, jar.outputs.files, javac.destinationDirectory.get().file("module-info.class"))

      doFirst {
        // before running, delete any previous output, otherwise jmod will complain. it does not appear that jmod provides
        // any `--force` or `--overwrite` flag.
        if (jmodOut.exists()) jmodOut.delete()

        // additionally, jmod will complain if anything specified on the classpath is a non-existent directory; in rare
        // cases (a kotlin-enabled module with no sources), the task will be there, but the outputs will not.
        outputClasses.forEach {
          it.get().asFile.toPath().toAbsolutePath().toFile().mkdirs()
        }
      }

      // start building arguments
      argumentProviders.add(CommandLineArgumentProvider {
        mutableListOf(
          "create",
          "--class-path",
          outputClasses.joinToString(":") { project.projectRelative(it.get().asFile.toPath()) },
          "--module-path",
          modulepath.asPath,
        ).apply {
          application?.let { app ->
            addAll(listOf("--main-class=${app.mainClass}"))
          }

          when (val version = project.version as? String) {
            null, "unspecified" -> {}
            else -> add("--module-version=\"$version\"")
          }

          // out path is last
          add(jmodOutPath)
        }
      })
    }
  }
}
