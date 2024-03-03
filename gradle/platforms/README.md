
## Gradle Conventions: Platforms

This directory provides a suite of Gradle sub-projects, each of which defines a [Java Platform][0] for use in Gradle
projects targeting the JVM.

Java Platforms are Gradle's way of dealing with [Maven BOMs][1]. BOMs can be imported and are expressed in a build as
_constraints_, which influence the dependency resolution process.

Platforms (or BOMs, or whatever) are just one way to constrain or define versions in Gradle, with the best route being
[version catalogs](../libs) for general use. Platforms can still be useful for several reasons, though:

- **You can avoid vulnerabilities** by constraining your transitive dependency graph to avoid CVEs. The platforms
  provided here do this out of the box and are upgraded frequently.

- **You can avoid duplicate downloads and processing** by aligning your dependency graph and eliding JARs and other
  artifacts which would normally be duplicates of others

[0]: https://docs.gradle.org/current/userguide/java_platform_plugin.html
[1]: https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#bill-of-materials-bom-poms
