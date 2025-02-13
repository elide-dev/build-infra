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

package dev.elide.infra.gradle.jmod

import dev.elide.infra.gradle.api.javaHomeFile
import dev.elide.infra.gradle.api.projectRelative
import dev.elide.infra.gradle.jpms.GradleJpmsPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.provider.Property
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.property
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import javax.inject.Inject

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
  // Implementation for DSL configuration related to jmod.
  internal abstract class ConfiguredJModExtension @Inject constructor (factory: ObjectFactory) : GradleJModExtension {
    // Defaults to enablement.
    override val enabled: Property<Boolean> = factory.property(Boolean::class).convention(true)
  }

  override fun apply(target: Project) {
    // always apply the JPMS plugin
    if (!target.plugins.hasPlugin(GradleJpmsPlugin::class.java))
      target.pluginManager.apply(GradleJpmsPlugin::class.java)

    // create the extension
    target.extensions.create(
      GradleJModExtension::class.java,
      GradleJModExtension.NAME,
      ConfiguredJModExtension::class.java,
    )

    // register jmod task
    val modulepath = requireNotNull(target.configurations.named("modulepath").get()).asPath
    target.tasks.register("jmod", JModTask::class.java) {
      group = "build"
      description = "Build a jmod artifact from this JVM project"

      val jmodConfig = target.extensions.getByType<GradleJModExtension>()
      enabled = jmodConfig.enabled.get()

      val dirOut = project.layout.buildDirectory.dir("jmod")
      val jmodOut = project.layout.buildDirectory.file("jmod/${project.name}.jmod").get().asFile
      val jmodOutPath = project.projectRelative(jmodOut.toPath())

      destinationDirectory.set(dirOut)
      outFile.set(jmodOut)
      outputs.file(jmodOut)
      executable(project.javaHomeFile("bin", "jmod"))

      val application = target.extensions.findByType(JavaApplication::class.java)
      val javac = target.tasks.named("compileJava", JavaCompile::class).get()
      val kotlinc = target.tasks.findByName("compileKotlinJvm")
      val jar = when (val jvmJar = target.tasks.findByName("jvmJar")) {
        null -> target.tasks.named("jar", Jar::class).get()
        else -> jvmJar as Jar
      }

      // jmod depends on the outputs of all jvm builds in this module.
      listOfNotNull(
        javac,
        jar,
        kotlinc,
        target.tasks.findByName("classes"),
        target.tasks.findByName("jvmMainClasses"),
      ).let {
        dependsOn(it)
        mustRunAfter(it)
      }

      // output classes from java and kotlin compile jobs are inputs
      val outputClasses = listOfNotNull(
        javac.destinationDirectory,
        kotlinc?.let {
          target.tasks.named("compileKotlinJvm", KotlinJvmCompile::class).get().destinationDirectory
        },
      )

      // the JAR is an input to force Gradle to write outputs, including `module-info.class`. the `module-info.class`
      // file is always required for a jmod build.
      inputs.files(outputClasses, jar.outputs.files, javac.destinationDirectory.get().file("module-info.class"))

      doFirst {
        // before running, delete any previous output, otherwise jmod will complain. it does not appear that jmod
        // provides any `--force` or `--overwrite` flag.
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
          action.get().action,
          "--class-path",
          outputClasses.joinToString(":") { project.projectRelative(it.get().asFile.toPath()) },
          "--module-path",
          modulepath,
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
