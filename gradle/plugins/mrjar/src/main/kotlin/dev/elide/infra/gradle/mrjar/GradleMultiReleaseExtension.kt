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

package dev.elide.infra.gradle.mrjar

import org.gradle.api.provider.Property

/**
 * # Gradle Multi-Release Extension
 *
 * Configuration DSL extension for the [GradleMultiReleaseJarPlugin]. This extension is installed when the plug-in is
 * applied, with starter conventions which are reasonable. The developer can customize these settings through the
 * `mrjar` block in their `build.gradle.kts` file.
 */
public interface GradleMultiReleaseExtension {
  public companion object {
    /** DSL name of this extension. */
    public const val NAME: String = "mrjar"
  }

  /** Whether to enable MRJAR functionality; if set to `false`, the plug-in is rendered inert. */
  public val enabled: Property<Boolean>

  /** Prefer modern JVMs; results in thinner JARs by omitting insignificant or outdated bytecode versions. */
  public val preferModern: Property<Boolean>

  /** Whether to enable all JDK releases, which includes non-LTS tiers. Defaults to `false`. */
  public val allReleases: Property<Boolean>

  /** Whether to optimize by dropping classes which are identical except for bytecode tier. Defaults to `true`. */
  public val optimize: Property<Boolean>
}
