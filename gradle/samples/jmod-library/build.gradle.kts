plugins {
  java
  `java-library`
  id("dev.elide.jmod")
}

description = "Builds a jmod artifact from a pure Java library"

java {
  withSourcesJar()
  withJavadocJar()
}
