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

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
  `embedded-kotlin`
  `kotlin-dsl`
  `kotlin-dsl-precompiled-script-plugins`
  `maven-publish`
}

group = "dev.elide.infra"

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21

  toolchain {
    languageVersion = JavaLanguageVersion.of(21)
  }
}

kotlin {
  explicitApi()

  compilerOptions {
    apiVersion = KotlinVersion.KOTLIN_1_9
    languageVersion = KotlinVersion.KOTLIN_1_9
    jvmTarget = JvmTarget.JVM_21
  }
}

dependencyLocking {
  lockAllConfigurations()
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
  implementation(core.plugin.testlogger)
  implementation(core.tomlj)
  implementation(infra.bundles.plugins)
  implementation(infra.plugin.gradle.publish)
  implementation(files(core.javaClass.superclass.protectionDomain.codeSource.location))
  implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
  implementation(files(infra.javaClass.superclass.protectionDomain.codeSource.location))

  testImplementation(gradleTestKit())
  testImplementation(core.bundles.asm)
  testImplementation(core.javapoet)
  testImplementation(core.kotlinpoet)
  testImplementation(libs.testing.junit.jupiter)
  testImplementation(libs.testing.junit.jupiter.engine)
  testImplementation(libs.testing.junit.jupiter.params)
  testImplementation(core.kotlin.test)
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
  }
}

val testing: Configuration by configurations.creating {
  extendsFrom(configurations.testApi.get())
}

val testJar by tasks.registering(Jar::class) {
  archiveClassifier = "test"
  from(sourceSets.test.get().output)
}

artifacts {
  add(testing.name, testJar)
}
