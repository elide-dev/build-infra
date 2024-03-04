@file:Suppress("UnstableApiUsage")

package dev.elide.infra.gradle.testing

import dev.elide.infra.gradle.BuildConstants
import dev.elide.infra.gradle.api.*
import dev.elide.infra.gradle.api.ElideBuild.ElideBuildDsl
import org.gradle.accessors.dm.LibrariesForCore
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.kotlin.dsl.the
import org.gradle.testing.base.TestingExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * # Multi-JVM Testing
 */
public class ElideMultiJvmTestingConventions : Plugin<Project> {
  override fun apply(target: Project) {
    // `java` plugin is always included
    target.pluginManager.apply(BuildConstants.KnownPlugins.JAVA)

    // `jvm-test-suite` plugin is always included
    target.pluginManager.apply(BuildConstants.KnownPlugins.JVM_TEST_SUITE)

    target.pluginManager.withPlugin(BuildConstants.KnownPlugins.JVM_TEST_SUITE) {
      target.afterEvaluate {
        setupMultiJvmTesting(extensions.getByType(ElideBuildDsl::class.java))
      }
    }
  }
}

// Setup tasks for testing against a range of JVM versions.
private fun Project.setupMultiJvmTesting(conventions: ElideBuildDsl) {
  val toolchain = javaToolchain()
  val javaToolchainService = serviceOf<JavaToolchainService>()
  val java = the<JavaPluginExtension>()
  val minimum = resolveJavaBytecodeMinimum(conventions)
  if (minimum == null) {
    logger.warn(
      "Cannot setup multi-JVM testing without JVM minimum set. Please set `conventions.jvm.minimum` or set the JVM " +
      "minimum target using the Build Infra DSL."
    )
    return
  }
  val target = resolveJavaBytecodeTarget(toolchain, java, conventions)
  val testing = the<TestingExtension>()
  val libs = the<LibrariesForLibs>()
  val core = the<LibrariesForCore>()

  // configure the base test suite
  val baseTestSuite = testing.suites.named("test", JvmTestSuite::class.java) {
    useJUnitJupiter(requireNotNull(libs.testing.junit.jupiter.asProvider().get().version) {
      "Failed to determine version for `testing-junit-jupiter`"
    })
    useKotlinTest(requireNotNull(core.kotlin.test.get().version) {
      "Failed to determine version for `kotlin-test`"
    })
  }.get()

  val checkTask = tasks.named("check")
  val baseTestTask = when {
    // test classes task is named differently for kotlin multiplatform projects
    plugins.hasPlugin(BuildConstants.KnownPlugins.KOTLIN_MULTIPLATFORM) ->
      tasks.named(BuildConstants.TaskName.TEST_JVM, Test::class.java)

    // for kotlin jvm or pure java projects, it is named `test`
    else -> tasks.named(BuildConstants.TaskName.TEST, Test::class.java)
  }.get()

  val testSuites = (minimum until target).all.toList().map {
    setupJvmTestTask(javaToolchainService, baseTestTask, baseTestSuite, testing, it)
  }

  checkTask.configure {
    dependsOn(testSuites)
  }
}

// Setup tasks for testing against a range of JVM versions.
private fun Project.setupJvmTestTask(
  toolchains: JavaToolchainService,
  baseTestTask: Test,
  base: JvmTestSuite,
  testing: TestingExtension,
  target: JvmTarget,
): JvmTestSuite {
  // build test suite name
  val testSuiteName = StringBuilder().apply {
    append("testJvm")
    append(target.target)
  }.toString()

  // resolve toolchain for target
  val launcher = javaLauncherAt(target, toolchains)

  testing.apply {
    return suites.create(testSuiteName, JvmTestSuite::class.java) {
      dependencies {
        // tbd
      }

      targets.all {
        testTask.configure {
          group = "verification"
          description = "Run testsuite on JVM ${target.target}"

          shouldRunAfter(base)
          javaLauncher.set(launcher)

          classpath = baseTestTask.classpath
          testClassesDirs = baseTestTask.testClassesDirs
        }
      }
    }
  }
}
