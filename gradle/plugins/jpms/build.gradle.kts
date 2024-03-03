plugins {
  `kotlin-dsl`
  id("infra.root")
  id("infra.gradle.plugin")
}

description = "Gradle Plugin for integration with the Java Platform Module System (JPMS)"

kotlin {
  explicitApi()
}

dependencies {
  api(gradleApi())
  api("dev.elide.infra:build-infra")
  api("dev.elide.infra:base")
}
