plugins {
  `java-gradle-plugin`
  `kotlin-dsl`
  id("infra.root")
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
