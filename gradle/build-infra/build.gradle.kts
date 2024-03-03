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
  implementation(infra.bundles.plugins)
  implementation(infra.plugin.gradle.publish)
  implementation(core.plugin.testlogger)
  implementation(core.plugin.kotlin.multiplatform)
  implementation(files(core.javaClass.superclass.protectionDomain.codeSource.location))
  implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
  implementation(files(infra.javaClass.superclass.protectionDomain.codeSource.location))

  testImplementation(gradleTestKit())
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
