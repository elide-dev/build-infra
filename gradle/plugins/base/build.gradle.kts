plugins {
  `kotlin-dsl`
  id("infra.root")
  id("infra.gradle.plugin")
  alias(core.plugins.testlogger)
}

description = "Gradle Plugin for establishing healthy project baseline settings"

kotlin {
  explicitApi()
}

java {
  withSourcesJar()
}

dependencies {
  api(gradleApi())
  api(core.plugin.kotlin.multiplatform)
  api("dev.elide.infra:build-infra")

//  testApi(group = "dev.elide.infra", name = "build-infra", classifier = "test")
  testImplementation(gradleTestKit())
  testImplementation(libs.testing.junit.jupiter)
  testImplementation(libs.testing.junit.jupiter.engine)
  testImplementation(libs.testing.junit.jupiter.params)
  testRuntimeOnly(libs.testing.junit.platform.console)
}

tasks.test {
  useJUnitPlatform()
}

gradlePlugin {
  plugins {
    create("base") {
      id = "dev.elide.base"
      implementationClass = "dev.elide.infra.gradle.GradleBaselinePlugin"
    }
  }
}
