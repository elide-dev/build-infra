package dev.elide.infra.gradle

/** Constants used throughout build infra tools. */
public object BuildConstants {
  /** Common/known plugin names. */
  public object KnownPlugins {
    /** Main Java plugin */
    public const val JAVA: String = "java"

    /** Java library plugin */
    public const val JAVA_LIBRARY: String = "java-library"

    /** Application plugin */
    public const val APPLICATION: String = "application"

    /** JVM test suite plugin */
    public const val JVM_TEST_SUITE: String = "jvm-test-suite"

    /** Groovy compiler plugin */
    public const val GROOVY: String = "groovy"

    /** Scala compiler plugin */
    public const val SCALA: String = "scala"

    /** Kotlin multiplatform plugin */
    public const val KOTLIN_MULTIPLATFORM: String = "org.jetbrains.kotlin.multiplatform"

    /** Kotlin serialization plugin */
    public const val KOTLIN_SERIALIZATION: String = "org.jetbrains.kotlin.plugin.serialization"

    /** Kotlin power-assert plugin */
    public const val KOTLIN_POWER_ASSERT: String = "com.bnorm.power.kotlin-power-assert"

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
  }

  /** Common/known plugin names. */
  public object Extensions {
    /** Extension name to use for actual build settings */
    public const val INFRA: String = "buildInfra"

    /** Extension name to use for meta-build-settings */
    public const val META: String = "infraPlugin"
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

    /** Property which allows overriding the JVM bytecode target */
    public const val JVM_TARGET: String = "conventions.jvm.target"

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

    // ---- System Properties -----------------------------------------------------------------------------------------

    /** Property hint for an IDEA sync */
    public const val IDEA_SYNC: String = "idea.active"
  }

  /** Environment variables which influence the build */
  public object Environment {
    // Nothing yet.
  }
}
