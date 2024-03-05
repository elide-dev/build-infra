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

@file:Suppress("UnstableApiUsage")

import com.adarshr.gradle.testlogger.theme.ThemeType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
  pmd
  `embedded-kotlin`
  `kotlin-dsl`
  `kotlin-dsl-precompiled-script-plugins`
  `maven-publish`
  `jvm-test-suite`
  `project-reports`
  `build-dashboard`

  alias(core.plugins.idea.ext)
  alias(core.plugins.kotlin.powerassert)
  alias(core.plugins.kotlin.serialization)
  alias(core.plugins.owasp)
  alias(core.plugins.sigstore)
  alias(core.plugins.spdx.sbom)
  alias(core.plugins.testlogger)
  alias(core.plugins.versions)
  alias(infra.plugins.cyclonedx)
  alias(infra.plugins.detekt)
  alias(infra.plugins.dokka)
  alias(infra.plugins.kover)
  alias(infra.plugins.spotless)
  alias(libs.plugins.dependencyAnalysis)
}

group = "dev.elide.infra"

// Build infra targets; does not apply to shipped/published targets.
val buildTimeJvmTarget = JvmTarget.JVM_21
val buildTimeJavaTarget = JavaVersion.VERSION_21
val buildTimeKotlinTarget = KotlinVersion.KOTLIN_1_9

java {
  sourceCompatibility = buildTimeJavaTarget
  targetCompatibility = buildTimeJavaTarget

  toolchain {
    languageVersion = JavaLanguageVersion.of(buildTimeJavaTarget.majorVersion)
  }
}

private fun KotlinJvmCompilerOptions.setupKotlinc() {
  apiVersion = buildTimeKotlinTarget
  languageVersion = buildTimeKotlinTarget
  jvmTarget = buildTimeJvmTarget
  allWarningsAsErrors = true
  progressiveMode = true
  javaParameters = true
}

kotlin {
  explicitApi()
  compilerOptions { setupKotlinc() }
}

tasks.withType(KotlinJvmCompile::class.java).configureEach {
  compilerOptions { setupKotlinc() }
}

dependencyLocking {
  lockAllConfigurations()
}

configurations.all {
  resolutionStrategy {
    activateDependencyLocking()
    enableDependencyVerification()
  }
}

listOf(Jar::class, Zip::class, Tar::class).forEach {
  tasks.withType(it).configureEach {
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
    if (this is Zip) isZip64 = true
  }
}

dependencies {
  implementation(core.kotlinx.collections.immutable)
  implementation(core.plugin.buildconfig)
  implementation(core.plugin.idea.ext)
  implementation(core.plugin.kotlin.multiplatform)
  implementation(core.plugin.kotlin.powerassert)
  implementation(core.plugin.owasp)
  implementation(core.plugin.proguard)
  implementation(core.plugin.sigstore)
  implementation(core.plugin.spdx.sbom)
  implementation(core.plugin.testlogger)
  implementation(core.tomlj)
  implementation(infra.bundles.plugins)
  implementation(infra.plugin.cyclonedx)
  implementation(infra.plugin.detekt)
  implementation(infra.plugin.dokka)
  implementation(infra.plugin.dokka.base)
  implementation(infra.plugin.gradle.publish)
  implementation(infra.plugin.kover)
  implementation(infra.plugin.spotless)
  implementation(libs.plugin.gr8)
  implementation(files(core.javaClass.superclass.protectionDomain.codeSource.location))
  implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
  implementation(files(infra.javaClass.superclass.protectionDomain.codeSource.location))

  testImplementation(gradleTestKit())
  testImplementation(core.bundles.asm)
  testImplementation(core.javapoet)
  testImplementation(core.kotlinpoet)
  testImplementation(core.kotlin.test)
  testImplementation(libs.testing.junit.jupiter)
  testImplementation(libs.testing.junit.jupiter.engine)
  testImplementation(libs.testing.junit.jupiter.params)
  testRuntimeOnly(libs.testing.junit.platform.console)
}

gradlePlugin {
  plugins {
    create("infra.settings") {
      id = "infra.settings"
      implementationClass = "dev.elide.infra.gradle.SettingsUmbrella"
    }
    create("infra.root") {
      id = "infra.root"
      implementationClass = "dev.elide.infra.gradle.RootBuildConvention"
    }
    create("infra.kotlin") {
      id = "infra.kotlin"
      implementationClass = "dev.elide.infra.gradle.ElideKotlin"
    }
    create("infra.kotlin.jvm") {
      id = "infra.kotlin.jvm"
      implementationClass = "dev.elide.infra.gradle.ElideKotlinJvm"
    }
    create("infra.gradle.plugin") {
      id = "infra.gradle.plugin"
      implementationClass = "dev.elide.infra.gradle.ElideGradlePlugin"
    }
    create("infra.library") {
      id = "infra.library"
      implementationClass = "dev.elide.infra.gradle.ElideLibraryConvention"
    }
    create("infra.catalog") {
      id = "infra.catalog"
      implementationClass = "dev.elide.infra.gradle.InfraCatalogConvention"
    }
    create("infra.multi-jvm-testing") {
      id = "infra.multi-jvm-testing"
      implementationClass = "dev.elide.infra.gradle.testing.ElideMultiJvmTestingConventions"
    }
  }
}

testlogger {
  theme = ThemeType.MOCHA_PARALLEL
  showPassed = true
  showFailed = true
  showSkipped = true
  slowThreshold = 30_000L
  isShowCauses = false
  isShowSimpleNames = true
  showStackTraces = false
  showExceptions = false
  showStandardStreams = false
}

dependencyCheck {
  System.getenv("NVD_API_KEY")?.ifBlank { null }?.let {
    nvd.apiKey = it
  }
}

val testing: Configuration by configurations.creating {
  extendsFrom(configurations.testApi.get())
}

testing {
  suites {
    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter(requireNotNull(libs.testing.junit.jupiter.asProvider().get().version))
      useKotlinTest(requireNotNull(core.kotlin.test.get().version))
    }
  }
}

val testJar by tasks.registering(Jar::class) {
  archiveClassifier = "test"
  from(sourceSets.test.get().output)
}

artifacts {
  add(testing.name, testJar)
}

val build: Task by tasks.getting {
  // Nothing at this time.
}

val check: Task by tasks.getting {
  dependsOn(
    tasks.koverVerify,
    tasks.detekt,
    tasks.pmdMain,
  )
}

val reports by tasks.registering {
  group = "reporting"
  description = "General all reports in all projects"

  dependsOn(
    tasks.named("projectReport"),
    tasks.named("koverXmlReport"),
    tasks.named("koverBinaryReport"),
  )
}
