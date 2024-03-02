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
  api("dev.elide.infra:jmod")
}
