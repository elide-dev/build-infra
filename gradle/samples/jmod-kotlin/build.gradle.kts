plugins {
  java
  `java-library`
  `embedded-kotlin`
  id("dev.elide.jmod")
}

description = "Builds a jmod artifact from a Java/Kotlin library"

java {
  withSourcesJar()
}

kotlin {
  // Nothing at this time.
}
