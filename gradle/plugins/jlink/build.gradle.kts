plugins {
  `kotlin-dsl`
  id("infra.root")
  id("infra.gradle.plugin")
}

description = "Gradle Plugin for using 'jlink' to create modular self-contained JVM applications"

kotlin {
  explicitApi()
}

dependencies {
  api(gradleApi())
  api("dev.elide.infra:build-infra")
  api("dev.elide.infra:base")
  api("dev.elide.infra:jpms")
  api("dev.elide.infra:jmod")
}
