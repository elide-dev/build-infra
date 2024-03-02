plugins {
  `java-gradle-plugin`
  `kotlin-dsl`
  id("infra.root")
}

description = "Gradle Plugin for producing bytecode-optimized Multi-Release JARs (MRJARs)"

kotlin {
  explicitApi()
}

dependencies {
  api(gradleApi())
  api("dev.elide.infra:build-infra")
  api("dev.elide.infra:base")
  api("dev.elide.infra:jpms")
}
