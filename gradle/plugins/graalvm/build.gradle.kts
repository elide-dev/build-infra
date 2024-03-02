plugins {
  `java-gradle-plugin`
  `kotlin-dsl`
  id("infra.root")
}

description = "Gradle Plugin for enhanced JPMS-enabled Native Image builds"

kotlin {
  explicitApi()
}

dependencies {
  api(gradleApi())
  api("dev.elide.infra:build-infra")
  api("dev.elide.infra:base")
  api("dev.elide.infra:jpms")
}
