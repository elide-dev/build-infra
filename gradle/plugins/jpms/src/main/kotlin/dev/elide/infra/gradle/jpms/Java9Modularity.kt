/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 *
 * The original source code of this file was obtained from the KotlinX Serialization project, here:
 * https://github.com/Kotlin/kotlinx.serialization/blob/master/buildSrc/src/main/kotlin/Java9Modularity.kt
 *
 * This code has been modified to be usable in a generic fashion. In accordance with the project's licensing, this file
 * is open-source and retains a JetBrains copyright.
 */

package dev.elide.infra.gradle.jpms

import dev.elide.infra.gradle.BuildConstants
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.*
import org.gradle.api.tasks.compile.*
import org.gradle.jvm.toolchain.*
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin.*
import org.gradle.process.*
import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.*
import org.jetbrains.kotlin.gradle.targets.jvm.*
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.tooling.core.*
import java.io.*
import java.util.*
import kotlin.reflect.*
import kotlin.reflect.full.*

/**
 * # Java 9 Modularity
 */
public object Java9Modularity {
  /**
   * Configure Java 9+ JPMS build tasks
   *
   * @param minimum Minimum JVM bytecode target
   * @param maximum Maximum JVM bytecode target
   * @param multiRelease Prepare a multi-release JAR
   * @param recompileBytecode Force bytecode re-compilation for each target version
   */
  @JvmStatic public fun Project.configureJPMS(
    minimum: JvmTarget,
    maximum: JvmTarget,
    multiRelease: Boolean = true,
    recompileBytecode: Boolean = false,
  ) {
    // @TODO(sgammon): implement more than just basic Java9 modularity
    configureJava9ModuleInfo(minimum, maximum, multiRelease, recompileBytecode)
  }

  /**
   * Configure Java 9+ JPMS build tasks
   *
   * @param minimum Minimum JVM bytecode target
   * @param maximum Maximum JVM bytecode target
   * @param multiRelease Prepare a multi-release JAR
   * @param recompileBytecode Force bytecode re-compilation for each target version
   */
  @JvmStatic
  @JvmOverloads
  public fun Project.configureJava9ModuleInfo(
    minimum: JvmTarget,
    maximum: JvmTarget,
    multiRelease: Boolean = true,
    recompileBytecode: Boolean = false,
  ) {
    // if JPMS is disabled manually, or IDEA is synchronizing, avoid JPMS changes completely
    val disableJPMS = this.rootProject.extra.has(BuildConstants.Properties.JPMS_DISABLED)
    val ideaActive = System.getProperty(BuildConstants.Properties.IDEA_SYNC) == "true"
    if (disableJPMS || ideaActive) return

    // resolve kotlin jvm targets
    val kotlin = extensions.findByType<KotlinProjectExtension>() ?: return
    val jvmTargets = kotlin.targets.filter { it is KotlinJvmTarget || it is KotlinWithJavaTarget<*, *> }
    if (jvmTargets.isEmpty()) {
      logger.info("No Kotlin JVM targets found, can't configure compilation of module-info!")
      return
    }

    // begin working with each target
    jvmTargets.forEach { target ->
      // grab the JAR target and add `Multi-Release`
      val artifactTask = tasks.getByName<Jar>(target.artifactsTaskName) {
        if (multiRelease) manifest {
          attributes("Multi-Release" to true)
        }
      }

      // for each compilation, locate a `module-info` from a parallel source set and build/verify it
      target.compilations.forEach { compilation ->
        val compileKotlinTask = compilation.compileTaskProvider.get() as KotlinCompile
        val defaultSourceSet = compilation.defaultSourceSet

        // derive the names of the source set and compile module task
        val sourceSetName = defaultSourceSet.name + "Module"
        kotlin.sourceSets.create(sourceSetName) {
          val sourceFile = this.kotlin.find { it.name == "module-info.java" }
          val targetDirectory = compileKotlinTask.destinationDirectory.map {
            it.dir("../${it.asFile.name}Module")
          }

          // only configure the compilation if necessary
          if (sourceFile != null) {
            // register and wire a task to verify module-info.java content
            //
            // this will compile the whole sources again with a JPMS-aware target Java version, so that the Kotlin
            // compiler can do the necessary verifications while compiling with `jdk-release=1.8` those verifications
            // are not done
            //
            // this task is only going to be executed when running with `check` or explicitly, not during normal build
            // operations
            val verifyModuleTask = registerVerifyModuleTask(
              compileKotlinTask,
              sourceFile
            )
            tasks.named("check") {
              dependsOn(verifyModuleTask)
            }

            // register a new compile module task
            val compileModuleTask = registerCompileModuleTask(
              compileKotlinTask,
              sourceFile,
              targetDirectory
            )

            // add the resulting module descriptor to this target's artifact
            artifactTask.from(compileModuleTask.map { it.destinationDirectory }) {
              if (multiRelease) {
                into("META-INF/versions/9/")
              }
            }
          } else logger.warn(
            "No module-info.java file found in ${this.kotlin.srcDirs}, can't configure compilation of module-info."
          )

          // remove the source set to prevent Gradle warnings
          kotlin.sourceSets.remove(this)
        }
      }
    }
  }

  /**
   * Add a Kotlin compile task that compiles `module-info.java` source file and Kotlin sources together, the Kotlin
   * compiler will parse and check module dependencies, but it currently won't compile to a `module-info.class` file.
   */
  private fun Project.registerVerifyModuleTask(
    compileTask: KotlinCompile,
    sourceFile: File
  ): TaskProvider<out KotlinJvmCompile> {
    apply<KotlinApiPlugin>()
    val capitalized = compileTask.name.removePrefix("compile")
      .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    val verifyModuleTaskName = "verify${capitalized}Module"

    // work-around for https://youtrack.jetbrains.com/issue/KT-60542
    val kotlinApiPlugin = plugins.getPlugin(KotlinApiPlugin::class)
    val verifyModuleTask = kotlinApiPlugin.registerKotlinJvmCompileTask(
      verifyModuleTaskName,
      compileTask.compilerOptions.moduleName.get()
    )

    verifyModuleTask {
      group = VERIFICATION_GROUP
      description = "Verify Kotlin sources for JPMS problems"

      libraries.from(compileTask.libraries)
      source(compileTask.sources)
      source(compileTask.javaSources)

      // part of workaround for https://youtrack.jetbrains.com/issue/KT-60541
      @Suppress("INVISIBLE_MEMBER")
      source(compileTask.scriptSources)
      source(sourceFile)
      destinationDirectory.set(temporaryDir)
      multiPlatformEnabled.set(compileTask.multiPlatformEnabled)

      compilerOptions {
        jvmTarget.set(JvmTarget.JVM_9)

        // to support LV override when set in aggregate builds
        languageVersion.set(compileTask.compilerOptions.languageVersion)

        // match opt-ins
        optIn.addAll(compileTask.kotlinOptions.options.optIn)

        freeCompilerArgs.addAll(listOf(
          "-Xjdk-release=9",
          "-Xsuppress-version-warnings",
          "-Xexpect-actual-classes",
        ))
      }

      // work-around for https://youtrack.jetbrains.com/issue/KT-60583
      inputs.files(
        libraries.asFileTree.elements.map { libs ->
          libs
            .filter { it.asFile.exists() }
            .map {
              zipTree(it.asFile).filter { it.name == "module-info.class" }
            }
        }
      ).withPropertyName("moduleInfosOfLibraries")

      this as KotlinCompile
      val kotlinPluginVersion = KotlinToolingVersion(kotlinApiPlugin.pluginVersion)
      if (kotlinPluginVersion <= KotlinToolingVersion("1.9.255")) {
        // part of work-around for https://youtrack.jetbrains.com/issue/KT-60541
        @Suppress("UNCHECKED_CAST")
        val ownModuleNameProp = (this::class.superclasses.first() as KClass<AbstractKotlinCompile<*>>)
          .declaredMemberProperties
          .find { it.name == "ownModuleName" }
          ?.get(this) as? Property<String>
        ownModuleNameProp?.set(compileTask.kotlinOptions.moduleName)
      }

      val taskKotlinLanguageVersion = compilerOptions.languageVersion.orElse(KotlinVersion.DEFAULT)
      @OptIn(InternalKotlinGradlePluginApi::class)
      if (taskKotlinLanguageVersion.get() < KotlinVersion.KOTLIN_2_0) {
        // part of work-around for https://youtrack.jetbrains.com/issue/KT-60541
        @Suppress("INVISIBLE_MEMBER")
        commonSourceSet.from(compileTask.commonSourceSet)
      } else {
        multiplatformStructure.refinesEdges.set(compileTask.multiplatformStructure.refinesEdges)
        multiplatformStructure.fragments.set(compileTask.multiplatformStructure.fragments)
      }

      // part of workaround for https://youtrack.jetbrains.com/issue/KT-60541
      // and work-around for https://youtrack.jetbrains.com/issue/KT-60582
      incremental = false
    }
    return verifyModuleTask
  }

  private fun Project.registerCompileModuleTask(
    compileTask: KotlinCompile,
    sourceFile: File,
    targetDirectory: Provider<out Directory>
  ) = tasks.register("${compileTask.name}Module", JavaCompile::class) {
    // configure the module compilation task.
    source(sourceFile)
    classpath = files()
    destinationDirectory.set(targetDirectory)

    // use a Java 11 toolchain with release 9 option because for some OS / architecture combinations, there are no JDK 9
    // builds available
    javaCompiler.set(this@registerCompileModuleTask.the<JavaToolchainService>().compilerFor {
      languageVersion.set(JavaLanguageVersion.of(11))
    })

    // build at desired release, at 9+
    options.release.set(9)

    options.compilerArgumentProviders.add(object : CommandLineArgumentProvider {
      @get:CompileClasspath val compileClasspath = compileTask.libraries
      @get:CompileClasspath val compiledClasses = compileTask.destinationDirectory

      @get:Input val moduleName = sourceFile
        .readLines()
        .single { it.contains("module ") }
        .substringAfter("module ")
        .substringBefore(' ')
        .trim()

      override fun asArguments() = mutableListOf(
        // provide the module path to the compiler instead of using a classpath. the module path should be the same as
        // the classpath of the compiler.
        "--module-path",
        compileClasspath.asPath,
        "--patch-module",
        "$moduleName=${compiledClasses.get()}",
        "-Xlint:-requires-transitive-automatic",
      )
    })
  }
}
