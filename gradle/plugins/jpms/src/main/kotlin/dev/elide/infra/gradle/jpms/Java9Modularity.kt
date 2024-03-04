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

@file:Suppress("DataClassPrivateConstructor")

package dev.elide.infra.gradle.jpms

import dev.elide.infra.gradle.BuildConstants
import dev.elide.infra.gradle.jpms.Java9Modularity.until
import org.gradle.api.*
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.*
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.*
import org.gradle.api.tasks.compile.*
import org.gradle.jvm.toolchain.*
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.language.base.plugins.LifecycleBasePlugin.*
import org.gradle.process.*
import org.jetbrains.annotations.VisibleForTesting
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
import java.util.stream.Stream
import kotlin.reflect.*
import kotlin.reflect.full.*

// Ordinal identity of JVM targets.
private val jvmOrdinalIdentity: Map<Int, JvmTarget> = listOf(
  JvmTarget.JVM_1_8,
  JvmTarget.JVM_9,
  JvmTarget.JVM_10,
  JvmTarget.JVM_11,
  JvmTarget.JVM_12,
  JvmTarget.JVM_13,
  JvmTarget.JVM_14,
  JvmTarget.JVM_15,
  JvmTarget.JVM_16,
  JvmTarget.JVM_17,
  JvmTarget.JVM_18,
  JvmTarget.JVM_19,
  JvmTarget.JVM_20,
  JvmTarget.JVM_21,
).associateBy {
  it.ordinal
}

// LTS releases which are included when building a range.
private val jvmLtsReleases: SortedSet<JvmTarget> = sortedSetOf(
  JvmTarget.JVM_1_8,
  JvmTarget.JVM_11,
  JvmTarget.JVM_17,
  JvmTarget.JVM_21,
)

// Resolve a source set name from either a Kotlin or Java source set.
private fun sourceSetName(subject: Any) = when (subject) {
  is SourceSet -> subject.name
  is KotlinSourceSet -> subject.name
  else -> error("Not a valid source set type: $subject (class: ${subject::class.java.name})")
}

// Filenames to exclude when merging MRJAR inputs.
private val jarMergeExcludes = sortedSetOf(
  "previous-compilation-data.bin",
  "MANIFEST.MF",
)

/**
 * # Modularity Configuration
 *
 * Defines configuration settings related to [Java9Modularity] logic
 *
 * @param minimum Minimum JVM target support
 * @param maximum Maximum JVM target support
 * @param multiRelease Whether to build a multi-release JAR
 * @param recompileBytecode Whether to enable bytecode re-compilation at each tier
 * @param allReleases Whether to include all releases (`true`), or just LTS releases (`false`)
 * @param enableKotlin Whether to enable Kotlin integration
 * @param sourceSetPrefix Prefix to use for generated source set names
 * @param sourceSetCategory Category (`main`, `test`, etc.) for the generated target source sets
 * @param replaceMainJar If `true`, replace the main JAR with the MRJAR target; if `false`, the MRJAR will be a new JAR
 *   with a classifier of `multirelease`.
 * @param preferModern Prefer the top-most modern Java tier as the default source set target; this will arrange a MRJAR
 *   with fallback versions, and a top-level-target main source set. Careful with this setting, as it means all classes
 *   must be available to override at a given version. This feature cannot be activated when Java 8 is included in the
 *   range of applicable targets.
 */
public data class ModularityConfig private constructor(
  public val minimum: JvmTarget,
  public val maximum: JvmTarget,
  public val multiRelease: Boolean = true,
  public val recompileBytecode: Boolean = true,
  public val allReleases: Boolean = false,
  public val enableKotlin: Boolean = true,
  public val sourceSetPrefix: String = "jvm",
  public val sourceSetCategory: String = "main",
  public val replaceMainJar: Boolean = false,
  public val preferModern: Boolean = false,
) {
  public val range: TargetRange get() = minimum until maximum

  public companion object {
    @JvmStatic @JvmOverloads public fun forRange(
      minimum: JvmTarget,
      maximum: JvmTarget,
      multiRelease: Boolean = true,
      recompileBytecode: Boolean = true,
      allReleases: Boolean = false,
      enableKotlin: Boolean = true,
      sourceSetPrefix: String = "jvm",
      sourceSetCategory: String = "main",
      replaceMainJar: Boolean = false,
      preferModern: Boolean = false
    ): ModularityConfig = ModularityConfig(
      minimum,
      maximum,
      multiRelease,
      recompileBytecode,
      allReleases,
      enableKotlin,
      sourceSetPrefix,
      sourceSetCategory,
      replaceMainJar,
      preferModern,
    )
  }
}

/**
 * # Java 9 Modularity
 *
 * JPMS utilities, adapted from KotlinX projects. Libraries like KotlinX Serialization use similar logic to package
 * multi-module capable Kotlin libraries. This logic has been modified to support full re-compilation at each targeted
 * bytecode tier.
 *
 * ## Usage in Original Form
 *
 * The `Java9Modularity` utility is popular in Kotlin projects. It is provided here, in unmodified form, via the method
 * [configureJava9ModuleInfo], which is exposed publicly. Aside from refactoring to allow sharing of logic, no changes
 * were made.
 */
public object Java9Modularity {
  /** Constants used by the Java 9 / JPMS features */
  public object Constants {
    /** Well-known relevant task names */
    public object TaskName {
      /** Main task for a "thin JAR," added if the main target is replaced with the MR JAR */
      public const val JAR_THIN: String = "jarThin"

      /** Main task for a "thin JAR," but in Kotlin Multiplatform mode */
      public const val JVM_JAR_THIN: String = "jvmJarThin"

      /** Fat (non-optimized) MR JAR, in JVM mode */
      public const val MERGED_MR_JAR: String = "mergedMultiReleaseJar"

      /** Fat (non-optimized) MR JAR, in Kotlin Multiplatform mode */
      public const val MERGED_MR_JAR_MULTIPLATFORM: String = "mergedMultiReleaseJvmJar"

      /** Optimized MR JAR, in JVM mode */
      public const val OPTIMIZED_MR_JAR: String = "multiReleaseJar"

      /** Optimized MR JAR, in Kotlin Multiplatform mode */
      public const val OPTIMIZED_MR_JAR_MULTIPLATFORM: String = "multiReleaseJvmJar"
    }

    /** Well-known JAR classifiers */
    public object Classifier {
      /** Targeted JAR classifier for top-level (default) target; used if the default target is replaced */
      public const val THIN: String = "thin"

      /** Multi-target JAR classifier; used if the default target is not replaced */
      public const val MULTI_RELEASE: String = "multirelease"

      /** Multi-target JAR classifier; used to emit the merged (non-optimized) MR JAR */
      public const val MULTI_RELEASE_FAT: String = "multirelease-all"
    }
  }

  /**
   * Configure Java/Kotlin non-modular builds with bytecode-targeted MRJARs.
   *
   * This method is intended for internal use, from conventions and plugins which aid in the setup MRJAR targets.
   *
   * @param config Configuration for the modularity build logic
   */
  @JvmStatic public fun Project.configureMultiReleaseJar(
    java: JavaPluginExtension,
    kotlin: KotlinProjectExtension?,
    config: ModularityConfig,
  ): Unit = config.range.let { range ->
    // resolve and check toolchain
    val (compiler, toolchain) = checkResolveJavaToolchain(range)

    // calculate the actual applicable targets
    val applicable = range.applicable(when (config.allReleases) {
      true -> sortedSetOf()
      false -> (jvmOrdinalIdentity.values.toSortedSet() - jvmLtsReleases).toSortedSet()
    }).toList()

    // resolve a base source set to inherit from
    val mainSourceSet: Any = resolveBaseSourceSet(java, kotlin)
    val toolchainVersion = toolchain.languageVersion.get().toString()
    logger.info("Configuring project as non-modular MRJAR (toolchain: Java ${toolchainVersion})")
    TODO("not yet implemented: non-modular MR JARs")
  }

  /**
   * Configure Java/Kotlin modular builds, potentially with bytecode-targeted MRJARs.
   *
   * This method is intended for internal use, from conventions and plugins which aid in the setup of JPMS and modular
   * MRJAR builds.
   *
   * @param modulepath Configuration to use for modulepath builds
   * @param moduleName Module name under build, if known; if not provided, best attempts are made to detect it
   * @param config Configuration for the modularity build logic
   */
  @JvmStatic public fun Project.configureModularity(
    java: JavaPluginExtension,
    kotlin: KotlinProjectExtension?,
    modulepath: Configuration,
    moduleName: String?,
    config: ModularityConfig,
  ): Unit = config.range.let { range ->
    // if JPMS is disabled manually, or IDEA is synchronizing, avoid JPMS changes completely
    val disableJPMS = this.rootProject.extra.has(BuildConstants.Properties.JPMS_DISABLED)
    val ideaActive = System.getProperty(BuildConstants.Properties.IDEA_SYNC) == "true"
    if (disableJPMS || ideaActive) return

    // calculate the actual applicable targets
    val (compiler, toolchain) = checkResolveJavaToolchain(range)
    val applicable = range.applicable(when (config.allReleases) {
      true -> sortedSetOf()
      false -> (jvmOrdinalIdentity.values.toSortedSet() - jvmLtsReleases).toSortedSet()
    }).toList()

    // decide on a base source set which the phantom sets will inherit from
    val toolchainVersion = toolchain.languageVersion.getOrElse(JavaLanguageVersion.of(config.maximum.target))
    logger.info("Configuring project as JPMS module '$moduleName' (toolchain: Java ${toolchainVersion})")
    val mainSourceSet: Any = resolveBaseSourceSet(java, kotlin)

    // build a suite of injected source sets; either in pure java, or in java-enabled kotlin
    val sourceSets = applicable.associate { level ->
      if (config.enableKotlin && kotlin != null)
        level to javaKotlinSourceSetForTarget(mainSourceSet as KotlinSourceSet, java, kotlin, modulepath, config, level)
      else
        level to javaSourceSetForTarget(mainSourceSet as SourceSet, java, modulepath, config, range, level)
    }

    // build a map of compile tasks corresponding to each source set
    val compileTasks = applicable.map { target ->
      if (!config.enableKotlin || kotlin == null)
        compilationsForJavaTarget(
          compiler,
          sourceSets,
          target,
          enableJpms = true,
        ).map {
          target to it
        }
      else
        TODO("kotlin / java tasks not yet configured")
    }.flatten().toMap()

    // finally, configure jvm targets and source sets with compile tasks
    val application = extensions.findByType(JavaApplication::class.java)
    configureModularJvmTargets(
      application,
      mainSourceSet,
      range,
      applicable,
      modulepath,
      sourceSets,
      compileTasks,
      config,
    )
  }

  // Build a range of JVM targets.
  @VisibleForTesting internal infix fun JvmTarget.until(to: JvmTarget): TargetRange {
    return JvmTargetRange((ordinal until (to.ordinal + 1)).map {
      requireNotNull(jvmOrdinalIdentity[it])
    }.toSortedSet())
  }

  // Holds a range of JVM targets, from a minimum to a maximum.
  @JvmInline internal value class JvmTargetRange(private val range: SortedSet<JvmTarget>): TargetRange {
    // Minimum supported JVM target.
    override val minimum: JvmTarget get() = range.first()

    // Maximum supported JVM target.
    override val maximum: JvmTarget get() = range.last()

    // Contains check.
    override operator fun contains(target: JvmTarget): Boolean = target in range

    // Stream of all supported JVM targets within the range.
    override val all: Stream<JvmTarget> get() = range.stream()

    // Stream of supported JVM targets within the range, without ignorable targets.
    override fun applicable(ignore: SortedSet<JvmTarget>): Stream<JvmTarget> = range.stream().filter {
      it !in ignore
    }
  }

  // Resolve the project's Java toolchain and compiler.
  @JvmStatic internal fun Project.checkResolveJavaToolchain(
    targets: TargetRange,
  ): Pair<Provider<JavaCompiler>, JavaToolchainSpec> {
    val javaToolchainService = project.serviceOf<JavaToolchainService>()
    val toolchain = project.extensions.getByType<JavaPluginExtension>().toolchain
    val compiler = javaToolchainService.compilerFor(toolchain)
    return compiler to toolchain.also {
      targets.all.forEach { target ->
        checkJavaToolchain(it, target)
      }
    }
  }

  // Check the Java toolchain for support at a sufficient tier for a target.
  @JvmStatic internal fun checkJavaToolchain(toolchain: JavaToolchainSpec, target: JvmTarget) {
    (toolchain.languageVersion.orNull ?: error(
      "Toolchain version undeclared; support cannot be guaranteed for target '$target'. " +
        "Please either set a Java toolchain level, or declare a maximum JVM target."
    )).also {
      require(JvmTarget.fromTarget(it.asInt().toString()) >= target) {
        "Effective JVM target candidate '$target' not supported by toolchain version at Java ${it.asInt()}. " +
          "Please either set a Java toolchain equal or greater to your maximum JVM target, or let the Build Infra " +
          "plugins select a toolchain for you."
      }
    }
  }

  // Resolve the Java or Kotlin base source set to inherit from, with regard to module builds.
  @Suppress("IMPLICIT_CAST_TO_ANY")
  @JvmStatic internal fun Project.resolveBaseSourceSet(
    java: JavaPluginExtension,
    kotlin: KotlinProjectExtension?,
  ): Any /* KotlinSourceSet | SourceSet */ {
    return when {
      // if there is no kotlin active, we should look for the `main` java source set as the base suite of sources
      kotlin == null -> java.sourceSets.named("main").get()

      // otherwise, we should account for kotlin multiplatform, which may create a `jvmMain` source set
      plugins.hasPlugin(BuildConstants.KnownPlugins.KOTLIN_MULTIPLATFORM) ->
        kotlin.sourceSets.named("jvmMain").get()

      // lastly, the older pure-Kotlin-JVM plugin should create a regular `main` source set
      plugins.hasPlugin(BuildConstants.KnownPlugins.KOTLIN_JVM) ->
        kotlin.sourceSets.named("main").get()

      // otherwise, we don't know what the base source set should be; fail hard
      else -> error("No way to resolve base source set, please file this as a bug")
    }.also {
      when (it) {
        is KotlinSourceSet -> logger.info("Using source set '${it.name}' as base for modular Java (type: Kotlin)")
        is SourceSet -> logger.info("Using source set '${it.name}' as base for modular Java (type: Pure Java)")
        else -> error("Invalid source set type: '${it::class.java.name}'")
      }
    }
  }

  // Spawn a phantom pure Java source set for the provided target.
  @JvmStatic internal fun Project.javaSourceSetForTarget(
    base: SourceSet,
    java: JavaPluginExtension,
    modulepath: Configuration,
    config: ModularityConfig,
    range: TargetRange,
    target: JvmTarget,
  ): SourceSet {
    // derive the names of the source set and compile module task
    val sourceSetName = StringBuilder().apply {
      // `jvm`
      append(config.sourceSetPrefix)
      // `jvm11`
      append(target.target)
    }.toString()

    logger.info("Creating multi-JVM target pure-Java source set at name '$sourceSetName'")

    // `main` or `test`, etc.
    val category = config.sourceSetCategory

    fun SourceSet.rebindSourceSetConfigurations() {
      compileClasspath = (modulepath + base.compileClasspath)
      runtimeClasspath = (modulepath + base.runtimeClasspath)
      annotationProcessorPath = base.annotationProcessorPath
    }
    return java.sourceSets.create(sourceSetName) {
      rebindSourceSetConfigurations()

      // use the base suite of source dirs, and also the override dir
      val baseDirs = base.java.srcDirs.toTypedArray()
      this.java.srcDirs(layout.projectDirectory.dir("src/$category/$sourceSetName"), *baseDirs)
    }
  }

  // Spawn a phantom Java/Kotlin source set for the provided target.
  @JvmStatic internal fun Project.javaKotlinSourceSetForTarget(
    base: KotlinSourceSet,
    java: JavaPluginExtension,
    kotlin: KotlinProjectExtension,
    modulepath: Configuration,
    config: ModularityConfig,
    target: JvmTarget,
  ): KotlinSourceSet {
    // derive the names of the source set and compile module task
    val sourceSetName = StringBuilder().apply {
      // `jvm`
      append(config.sourceSetPrefix)
      // `jvm11`
      append(target.target)
    }.toString()

    fun KotlinSourceSet.rebindSourceSetConfigurations() {
      // nothing yet
    }
    return kotlin.sourceSets.create(sourceSetName) {

    }
  }

  // Spawn or resolve compilation tasks for the provided target.
  @JvmStatic internal fun Project.compilationsForJavaTarget(
    compiler: Provider<JavaCompiler>,
    sourceSets: Map<JvmTarget, Any>,
    target: JvmTarget,
    enableJpms: Boolean,
  ): List<Task> {
    val projectName = this.name
    val compileTask = requireNotNull(sourceSets[target]) { "Failed to resolve source set for target '$target'" }.let { sourceSet ->
      require(sourceSet is SourceSet)
      tasks.named(sourceSet.compileJavaTaskName, JavaCompile::class.java)
    }

    return listOf(compileTask.get().apply {
      doFirst { logger.info("Compiling project '${projectName}' at JVM ${target.target}") }
      javaCompiler = compiler
      modularity.inferModulePath.set(enableJpms)
      sourceCompatibility = target.target
      targetCompatibility = target.target
    })
  }

  /**
   * Configure Java/Kotlin modular builds, potentially with bytecode-targeted MRJARs.
   *
   * This method is callable through [configureModularity], in particular from conventions and plugins which aid in the
   * setup of JPMS and MRJAR tasks.
   *
   * @param range Range of JVM support
   * @param applicable Effective applicable targets to build for
   * @param sourceSetMap Map of generated source sets
   * @param taskMap Map of generated compile tasks
   * @param config Configuration for the module generator logic
   */
  private fun Project.configureModularJvmTargets(
    application: JavaApplication?,
    mainSourceSet: Any,
    range: TargetRange,
    applicable: List<JvmTarget>,
    modulepath: Configuration,
    sourceSetMap: Map<JvmTarget, Any>,
    taskMap: Map<JvmTarget, Task>,
    config: ModularityConfig,
  ) {
    // resolve kotlin jvm targets and java project extension
    val buildDependencies = LinkedList<Any>()
    val java = extensions.findByType<JavaPluginExtension>()
    val kotlin = if (!config.enableKotlin) null else extensions.findByType<KotlinProjectExtension>()

    // detect whether kotlin multiplatform is enabled; it influences the target and source set names
    val (isKotlin, isKotlinMultiplatform) = when {
      plugins.hasPlugin(BuildConstants.KnownPlugins.KOTLIN_MULTIPLATFORM) -> true to true
      plugins.hasPlugin(BuildConstants.KnownPlugins.KOTLIN_JVM) -> true to false
      else -> false to false
    }
    val kotlinJvmTargets = kotlin
      ?.targets
      ?.filter { it is KotlinJvmTarget || it is KotlinWithJavaTarget<*, *> }
      ?: emptyList()

    if (java == null && (kotlinJvmTargets.isEmpty() || !isKotlin)) {
      logger.warn("No Java or Kotlin JVM targets found, can't configure compilation of modular Java")
      return
    }

    // account for multi-platform kotlin, which emits a `jvmJar`
    val jar = tasks.findByName("jar") as? Jar
      ?: if (!config.enableKotlin) null else tasks.findByName("jvmJar")
        as? Jar

    if (jar == null) {
      logger.warn("No JAR task found at name `jar` or `jvmJar` for project '${this.name}'")
      return
    }

    // resolve the compile task for the main source set
    val baseSourceSetCompileTask: TaskProvider<Task> = when {
      isKotlinMultiplatform -> tasks.named(BuildConstants.TaskName.COMPILE_KOTLIN_JVM)
      isKotlin -> tasks.named(BuildConstants.TaskName.COMPILE_KOTLIN)
      else -> tasks.named(BuildConstants.TaskName.COMPILE_JAVA)
    }
    buildDependencies.add(baseSourceSetCompileTask)

    // resolve main source set name
    val mainSourceSetName = sourceSetName(mainSourceSet)

    // if we are replacing the default jar, we need to add a "thin jar" task which produces the same output
    val baseJar = if (config.replaceMainJar) {
      val classesTask = requireNotNull(
        if (mainSourceSetName == "main") "classes" else tasks.named("${mainSourceSetName}Classes")
      ) { "No compilation for top-level JVM target. This is probably a bug in the Build Infra plugins." }

      val thinJarName = if (isKotlinMultiplatform) Constants.TaskName.JVM_JAR_THIN else Constants.TaskName.JAR_THIN
      tasks.create(thinJarName, Jar::class) {
        val maxTarget = range.maximum.target
        group = "build"
        description = "Thin JAR built at the maximal Java target ($maxTarget); original 'jar' before replacement"
        archiveClassifier = Constants.Classifier.THIN
        isZip64 = true
        entryCompression = ZipEntryCompression.STORED
        dependsOn(baseSourceSetCompileTask, classesTask)
        val outs = baseSourceSetCompileTask.get().outputs.files.files
        inputs.files(outs)
        from(outs) {
          exclude("previous-compilation-data.bin")
        }
      }.also {
        buildDependencies.add(it)
      }
    } else {
      // if we _are not_ replacing the base JAR, we should use it as the source of truth for the main source set's build
      // outputs.
      jar
    }

    // create a `jarJvm<>Thin` target for each jvm tier we intend to compile for, except the top-most tier, because that
    // is the regular `jarThin` target
    val jarThinTasks = applicable.map { target ->
      val jarThinBaseName = if (isKotlinMultiplatform) Constants.TaskName.JVM_JAR_THIN else Constants.TaskName.JAR_THIN
      val jarThinAtTargetName = StringBuilder().apply {
        // `jarThin` / `jvmJarThin`
        append(jarThinBaseName)

        // `jarThinJvm17` / `jvmJarThinJvm17`
        append("Jvm${target.target}")
      }.toString()

      // `jvm17`
      val classifier = "jvm${target.target}"

      // compilation and source set for this run
      val sourceSet = requireNotNull(sourceSetMap[target]) { "No source set for applicable target $target" }
      val sourceSetName = sourceSetName(sourceSet)

      val compilation = requireNotNull(taskMap[target]) { "No compilation for applicable target '$target'" }
      val classesTask = requireNotNull(
        tasks.named(if (sourceSetName == "main") "classes" else "${sourceSetName}Classes").get()
      ) { "No compilation for applicable target '$target'" }

      target to tasks.register(jarThinAtTargetName, Jar::class) {
        group = "build"
        description = "Thin JAR built specifically for JVM ${target.target}"
        archiveClassifier = classifier
        isZip64 = true
        entryCompression = ZipEntryCompression.STORED

        dependsOn(classesTask, baseSourceSetCompileTask)
        val outs = compilation.outputs.files.files
        inputs.files(outs)
        from(outs) {
          exclude("previous-compilation-data.bin")
        }
      }.also {
        buildDependencies.add(it)
      }
    }

    // if we need to create a multi-release jar, we spawn our task here to do so
    if (config.multiRelease) {
      val multiReleaseJarName = if (isKotlinMultiplatform)
        Constants.TaskName.MERGED_MR_JAR_MULTIPLATFORM
      else
        Constants.TaskName.MERGED_MR_JAR

      // build a merged jar with all classes at all levels
      val multiReleaseFatJar = tasks.register(multiReleaseJarName, Jar::class) {
        // we are going to merge from each of the thin jar tasks
        archiveClassifier = Constants.Classifier.MULTI_RELEASE_FAT
        entryCompression = ZipEntryCompression.STORED
        manifest { attributes("Multi-Release" to true) }
        dependsOn(baseJar, jarThinTasks.map { it.second })

        val zipTreeSources = jarThinTasks.filter {
          // filter out the base merged JAR, because we need to position it differently in the final product (at the
          // root of the jar, instead of a versioned path).
          !(config.preferModern && it.first == range.maximum || (!config.preferModern && it.first == range.minimum))
        }.map { (target, task) ->
          val files = task.get().outputs.files
          Triple(target, files.files, zipTree(task.get().outputs.files.singleFile))
        }

        inputs.files(
          zipTreeSources.map { it.second }.flatten()
        )

        // from each thin jar, as a zip tree, include the tree of files at the designated version
        zipTreeSources.map { (target, _, bundle) ->
          from(bundle) {
            into("META-INF/versions/${target.target}/")
            exclude { it.name in jarMergeExcludes || it.isDirectory }
          }
        }
      }.also {
        buildDependencies.add(it)
      }

      val optimizedMultiReleaseJarName = if (isKotlinMultiplatform)
        Constants.TaskName.OPTIMIZED_MR_JAR_MULTIPLATFORM
      else
        Constants.TaskName.OPTIMIZED_MR_JAR

      // optimize the fat merged jar with compression, and by dropping identical classes from higher levels of support
      tasks.register(optimizedMultiReleaseJarName, Jar::class) {
        group = "build"
        description = "Build merged MRJAR, optimized for all target JVMs"
        entryCompression = ZipEntryCompression.DEFLATED
        inputs.files(multiReleaseFatJar.get().outputs.files)
        dependsOn(multiReleaseFatJar, baseJar)

        // if we aren't replacing the main jar, we should use the multi-release classifier
        if (!config.replaceMainJar) archiveClassifier = Constants.Classifier.MULTI_RELEASE

        manifest {
          // merge with base jar manifest
          attributes(baseJar.manifest.attributes.toMap())
          attributes("Multi-Release" to true)
          application?.mainClass?.orNull?.ifBlank { null }?.let {
            attributes("Main-Class" to it)
          }
        }

        val minimalTarget = jarThinTasks.first().second
        val maximalTarget = jarThinTasks.last().second
        val multiReleaseJarZip = zipTree(multiReleaseFatJar.get().outputs.files.singleFile)
        val baseJarZip = zipTree(baseJar.outputs.files.singleFile)
        val primaryJarZip =
          if (!config.preferModern)
            zipTree(minimalTarget.get().outputs.files.singleFile)
          else
            zipTree(maximalTarget.get().outputs.files.singleFile)

        // from the primary jar (either the most or least modern tier thin jar), merge the classes into the root of the
        // jar as these should be the default compiled set. this time, we should be excluding everything under
        // `META-INF`, as this should be under the control of the user's jar.
        //
        // we place these first in legacy mode, because older runtimes don't check the `META-INF` version root (JDK 8).
        // in such cases, we are "progressively enhancing" on the base class suite.
        if (!config.preferModern) from(primaryJarZip) {
          exclude { it.name.startsWith("META-INF") || it.isDirectory }
        }

        // depending on compat mode, the lowest or highest support tier will be the base class set; the `jar` tool will
        // not necessarily detect it in the list of "supported releases," because it doesn't have a dedicated `versions`
        // directory.
        //
        // we can trick the `jar` tool into detecting support for the base level by copying the `module-info.class` into
        // the base target's version directory, which has minimal size impact. including an identical `module-info` at
        // this position is legal according to the MRJAR spec, and may even be faster, because the target runtime will
        // not need to fall back to the base when loading the module.
        //
        // we place this file first in the final jar META-INF, because it will sort after the manifest anyway, so it
        // will end up in version order with the members of `mergedMultiReleaseFatJar`.
        from(primaryJarZip) {
          include("module-info.class")
          into("META-INF/versions/${range.minimum.target}/")
        }

        // from the merged jar, and then from the base jar, assemble our final optimized jar
        from(multiReleaseJarZip) {
          // exclude input `MANIFEST.MF` files and other stuff we don't need
          exclude { it.name in jarMergeExcludes || it.isDirectory }
        }

        // from the user's jar, merge in all non-class entities. this includes processed JAR resources, which are not
        // included in jvm source sets created by this plugin.
        from(baseJarZip) {
          exclude { it.name in jarMergeExcludes || it.isDirectory || it.name.endsWith(".class") }
        }

        // followup to step one: if we *are* preferring modern modes, place the base suite at the bottom of the jar zip.
        if (config.preferModern) from(primaryJarZip) {
          exclude { it.name.startsWith("META-INF") || it.isDirectory }
        }
      }.also {
        buildDependencies.add(it)
      }
    }

    // begin working with each kotlin target; if we have no kotlin targets, or kotlin integration is disabled, we can
    // exit early.
    if (!config.enableKotlin || kotlin == null) return

//    kotlinJvmTargets.forEach { target ->
//      configureKotlinModularity(target, config.multiRelease, kotlin)
//    }
  }

  /**
   * Configure Java 9+ JPMS build tasks; this is the original implementation of the method, as provided in JetBrains
   * KotlinX projects.
   *
   * It is provided here so it may be used downstream where needed.
   *
   * @param multiRelease Whether to build a multi-release JAR
   */
  @JvmStatic
  @JvmOverloads
  public fun Project.configureJava9ModuleInfo(multiRelease: Boolean = true) {
    // if JPMS is disabled manually, or IDEA is synchronizing, avoid JPMS changes completely
    val disableJPMS = this.rootProject.extra.has(BuildConstants.Properties.JPMS_DISABLED)
    val ideaActive = System.getProperty(BuildConstants.Properties.IDEA_SYNC) == "true"
    if (disableJPMS || ideaActive) return

    // resolve kotlin jvm targets and java project extension
    val kotlin = extensions.findByType<KotlinProjectExtension>() ?: return
    val kotlinJvmTargets = kotlin
      .targets
      .filter { it is KotlinJvmTarget || it is KotlinWithJavaTarget<*, *> }

    if (kotlinJvmTargets.isEmpty()) {
      logger.warn("No Kotlin JVM targets found, can't configure compilation of `module-info`")
      return
    }

    // begin working with each target
    kotlinJvmTargets.forEach { target ->
      configureModularity(target, multiRelease, kotlin)
    }
  }

  private fun Project.configureModularity(target: KotlinTarget, multiRelease: Boolean, kotlin: KotlinProjectExtension) {
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
          val verifyModuleTask = registerKotlinVerifyModuleTask(
            compileKotlinTask,
            sourceFile
          )
          tasks.named("check") {
            dependsOn(verifyModuleTask)
          }

          // register a new compile module task
          val compileModuleTask = registerKotlinCompileModuleTask(
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

  /**
   * Add a Kotlin compile task that compiles `module-info.java` source file and Kotlin sources together, the Kotlin
   * compiler will parse and check module dependencies, but it currently won't compile to a `module-info.class` file.
   */
  private fun Project.registerKotlinVerifyModuleTask(
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
              zipTree(it.asFile).filter { file -> file.name == "module-info.class" }
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

  private fun Project.registerKotlinCompileModuleTask(
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
    javaCompiler.set(this@registerKotlinCompileModuleTask.the<JavaToolchainService>().compilerFor {
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
