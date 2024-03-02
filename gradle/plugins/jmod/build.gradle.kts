plugins {
  `java-gradle-plugin`
  `kotlin-dsl`

  id("infra.root")
}

kotlin {
  explicitApi()
}

dependencies {
  api(gradleApi())
  api("dev.elide.infra:base")
  api("dev.elide.infra:jpms")
}

gradlePlugin {
  plugins {
    create("jmod") {
      id = "dev.elide.jmod"
      implementationClass = "dev.elide.infra.gradle.jmod.GradleJModPlugin"
    }
  }
}
