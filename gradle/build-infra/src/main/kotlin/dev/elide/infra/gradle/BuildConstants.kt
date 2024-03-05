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

package dev.elide.infra.gradle

/** Constants used throughout build infra tools. */
public object BuildConstants {
  /** Common/known plugin names. */
  public object KnownPlugins {
    /** Built-in Gradle base plugin */
    public const val BASE: String = "base"

    /** Main Java plugin */
    public const val JAVA: String = "java"

    /** Java library plugin */
    public const val JAVA_LIBRARY: String = "java-library"

    /** Application plugin */
    public const val APPLICATION: String = "application"

    /** JVM test suite plugin */
    public const val JVM_TEST_SUITE: String = "jvm-test-suite"

    /** Build Dashboard plugin */
    public const val BUILD_DASHBOARD: String = "build-dashboard"

    /** Project Reports plugin */
    public const val PROJECT_REPORTS: String = "project-reports"

    /** Test report aggregation */
    public const val TEST_REPORT_AGGREGATION: String = "test-report-aggregation"

    /** JaCoCo report aggregation */
    public const val JACOCO_REPORT_AGGREGATION: String = "jacoco-report-aggregation"

    /** Groovy compiler plugin */
    public const val GROOVY: String = "groovy"

    /** Scala compiler plugin */
    public const val SCALA: String = "scala"

    /** Kotlin multiplatform plugin */
    public const val KOTLIN_MULTIPLATFORM: String = "org.jetbrains.kotlin.multiplatform"

    /** Kotlin JVM plugin */
    public const val KOTLIN_JVM: String = "org.jetbrains.kotlin.jvm"

    /** Kotlin serialization plugin */
    public const val KOTLIN_SERIALIZATION: String = "org.jetbrains.kotlin.plugin.serialization"

    /** Kotlin power-assert plugin */
    public const val KOTLIN_POWER_ASSERT: String = "com.bnorm.power.kotlin-power-assert"

    /** Plugin for creating Gradle plugins */
    public const val JAVA_GRADLE_PLUGIN: String = "java-gradle-plugin"

    /** Plugin for publishing Gradle plugins */
    public const val GRADLE_PLUGIN_PUBLISH: String = "com.gradle.plugin-publish"

    /** Built-in Kotlin DSL plugin */
    public const val KOTLIN_DSL: String = "kotlin-dsl"

    /** Built-in Kotlin DSL precompiled scripts plugin */
    public const val KOTLIN_PRECOMPILED_SCRIPTS: String = "kotlin-dsl-precompiled-script-plugins"

    /** Infra plug-in: base */
    public const val INFRA_BASE: String = "dev.elide.base"

    /** Infra plug-in: JPMS */
    public const val INFRA_JPMS: String = "dev.elide.jpms"

    /** Infra plug-in: jmod */
    public const val INFRA_JMOD: String = "dev.elide.jmod"

    /** Infra plug-in: jlink */
    public const val INFRA_JLINK: String = "dev.elide.jlink"

    /** Infra plug-in: multi-release JARs */
    public const val INFRA_MRJAR: String = "dev.elide.mrjar"

    /** Infra plug-in: GraalVM */
    public const val INFRA_GRAALVM: String = "dev.elide.graalvm"

    /** Scala compiler plugin */
    public const val TEST_LOGGER: String = "com.adarshr.test-logger"

    /** Settings plug-in: Gradle Enterprise */
    public const val SETTINGS_GRADLE_ENTERPRISE: String = "com.gradle.enterprise"

    /** Settings plug-in: Buildless */
    public const val SETTINGS_BUILDLESS: String = "build.less"

    /** Settings plug-in: Foojay Toolchains resolver */
    public const val SETTINGS_FOOJAY_TOOLCHAINS: String = "org.gradle.toolchains.foojay-resolver-convention"

    /** Settings plug-in: Gradle common user settings */
    public const val SETTINGS_GRADLE_COMMON: String = "com.gradle.common-custom-user-data-gradle-plugin"

    /** Settings plug-in: Micronaut Catalog plugin */
    public const val SETTINGS_MICRONAUT_CATALOG: String = "io.micronaut.platform.catalog"
  }

  /** Common/known plugin names. */
  public object Extensions {
    /** Extension name to use for actual build settings */
    public const val INFRA: String = "buildInfra"

    /** Extension name to use for meta-build-settings */
    public const val META: String = "infraPlugin"

    /** Extension for customizing catalogs */
    public const val CATALOG: String = "infraCatalog"
  }

  /** Plug-in IDs for conventions */
  public object Conventions {
    /** Settings-time configurations */
    public const val SETTINGS: String = "elide.settings"

    /** Root build configurations */
    public const val ROOT_BUILD: String = "elide.root"

    /** General JVM conventions, with Java support */
    public const val JVM: String = "elide.jvm"

    /** General Kotlin support, regardless of target platform */
    public const val KOTLIN: String = "elide.kotlin"

    /** Kotlin Multiplatform JVM targets */
    public const val KOTLIN_JVM: String = "elide.kotlin.jvm"
  }

  /** Properties which configure the build */
  public object Properties {
    // ---- Build Properties ------------------------------------------------------------------------------------------

    /** Property which allows overriding the JVM maximum bytecode target */
    public const val JVM_TARGET: String = "conventions.jvm.target"

    /** Property which allows overriding the JVM minimum bytecode target */
    public const val JVM_MINIMUM: String = "conventions.jvm.minimum"

    /** Property which allows overriding the JVM toolchain target */
    public const val JVM_TOOLCHAIN: String = "conventions.jvm.toolchain"

    /** Kotlin SDK version override */
    public const val KOTLIN_VERSION: String = "conventions.kotlin.version"

    /** Kotlin API version override */
    public const val KOTLIN_API: String = "conventions.kotlin.api"

    /** Kotlin language version override */
    public const val KOTLIN_LANGUAGE: String = "conventions.kotlin.language"

    /** Kotlin JVM bytecode target version override */
    public const val KOTLIN_JVM_TARGET: String = "conventions.kotlin.jvm.target"

    /** JPMS kill-switch */
    public const val JPMS_DISABLED: String = "conventions.jvm.disableJpms"

    /** Enables test exception printing */
    public const val TEST_EXCEPTIONS: String = "conventions.testing.logger.exceptions"

    /** Enables test stdout */
    public const val TEST_LOGS: String = "conventions.testing.logger.logs"

    /** Controls how test names are shown; use `simple` or `full` (default: `simple`) */
    public const val TEST_NAMES: String = "conventions.testing.logger.names"

    // ---- System Properties -----------------------------------------------------------------------------------------

    /** Property hint for an IDEA sync */
    public const val IDEA_SYNC: String = "idea.active"
  }

  /** Well-known names of important source sets */
  public object SourceSets {
    /** JVM main source set (Kotlin JVM, or pure Java). */
    public const val MAIN: String = "main"

    /** JVM test source set (Kotlin JVM, or pure Java). */
    public const val TEST: String = "test"

    /** JVM main source set (Kotlin Multiplatform). */
    public const val JVM_MAIN: String = "jvmMain"

    /** JVM test source set (Kotlin Multiplatform). */
    public const val JVM_TEST: String = "jvmTest"
  }

  /** Well-known names of important Gradle configurations */
  public object Configurations {
    /** Traditional compilation classpath */
    public const val COMPILE_CLASSPATH: String = "compileClasspath"

    /** Traditional runtime classpath */
    public const val RUNTIME_CLASSPATH: String = "runtimeClasspath"

    /** Java 9+ JPMS module path */
    public const val MODULEPATH: String = "modulepath"
  }

  /** Well-known names of important tasks */
  public object TaskName {
    /** General Java compile task */
    public const val COMPILE_JAVA: String = "compileJava"

    /** General Kotlin compile task */
    public const val COMPILE_KOTLIN: String = "compileKotlin"

    /** Kotlin multiplatform JVM compile task */
    public const val COMPILE_KOTLIN_JVM: String = "compileKotlinJvm"

    /** General Java/Kotlin test task */
    public const val TEST: String = "test"

    /** JVM Java/Kotlin test task in a Kotlin Multiplatform project */
    public const val TEST_JVM: String = "jvmTest"

    /** Detekt merge task for SARIF reporting */
    public const val DETEKT_MERGE_SARIF: String = "detektMergeSarif"

    /** Detekt merge task for XML reporting */
    public const val DETEKT_MERGE_XML: String = "detektMergeXml"
  }

  /** Environment variables which influence the build */
  public object Environment {
    /** Variable which enables test exception printing */
    public const val TEST_EXCEPTIONS: String = "TEST_EXCEPTIONS"

    /** Variable which enables test stdout */
    public const val TEST_LOGS: String = "TEST_LOGS"

    /** Variable controlling how test names are shown; use `simple` or `full` (default: `simple`) */
    public const val TEST_NAMES: String = "TEST_NAMES"
  }
}
