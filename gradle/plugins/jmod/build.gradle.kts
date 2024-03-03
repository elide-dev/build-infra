plugins {
  `kotlin-dsl`
  id("infra.root")
  id("infra.gradle.plugin")
}

description = "Gradle Plugin for building modular 'jmod' artifacts alongside JARs"

kotlin {
  explicitApi()
}

dependencies {
  api(gradleApi())
  api("dev.elide.infra:build-infra")
  api("dev.elide.infra:base")
  api("dev.elide.infra:jpms")

  testImplementation(gradleTestKit())
  testImplementation(libs.testing.junit.jupiter)
  testImplementation(libs.testing.junit.jupiter.engine)
  testImplementation(libs.testing.junit.jupiter.params)
  testRuntimeOnly(libs.testing.junit.platform.console)
}

gradlePlugin {
  plugins {
    create("jmod") {
      id = "dev.elide.jmod"
      implementationClass = "dev.elide.infra.gradle.jmod.GradleJModPlugin"
    }
  }
}
