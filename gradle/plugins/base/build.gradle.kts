plugins {
  `java-gradle-plugin`
  `kotlin-dsl`
  id("infra.root")
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
}

gradlePlugin {
  plugins {
    create("base") {
      id = "dev.elide.base"
      implementationClass = "dev.elide.infra.gradle.GradleBaselinePlugin"
    }
  }
}
