plugins {
  `java-gradle-plugin`
  `kotlin-dsl`
  id("infra.root")
}

kotlin {
  explicitApi()
}

java {
  withSourcesJar()
}

dependencies {
  api(gradleApi())
  api(core.plugin.kotlin.multiplatform)
}

gradlePlugin {
  plugins {
    create("base") {
      id = "dev.elide.base"
      implementationClass = "dev.elide.infra.gradle.GradleBaselinePlugin"
    }
  }
}
