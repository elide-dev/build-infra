
## Gradle JMod Plugin

This plugin enables support for building [`jmod`][0] artifacts in Gradle JVM projects. Java module artifacts can be used
at compile time, similar to JARs, but support a lot more artifact ephemera: header files, man files, native linkage, and
native command binaries, to name a few.

Binary Java module artifacts can also be used with [`jlink`][1] to produce an optimized stand-alone binary for a modular
Java application. Using the [JLink Plugin](../jlink), such outputs can be built with `jmod` artifacts as dependencies,
even when `classpath` use is involved.

### Installation

**`build.gradle.kts`**:
```kotlin
plugins {
    id("dev.elide.jmod") version "..."
}
```

**`libs.versions.toml`**:
```toml
[versions]
elide-infra = "..."

[plugins]
jmod = { id = "dev.elide.jmod", version.ref = "elide-infra" }
```

**`build.gradle.kts`** (using a [Version Catalog][3]):
```kotlin
plugins {
    alias(libs.plugins.jmod)
}
```

### Usage

The plug-in adds the `jmod` task, which builds an artifact at `<project>/build/jmod/<name>.jmod`. You can easily run the
task with `./gradlew jmod`. The added `jmod` task attaches to `build`, so `./gradlew build` will also produce the
artifact.

To configure the task:

**`build.gradle.kts`**
```kotlin
jmod {
    // See table of configuration properties.
}

tasks.jmod {
    // The individual task can also be configured like any Exec task.
    argumentProviders.add(CommandLineArgumentProvider {
        listOf("--more-arguments-to-jmod")
    })
}
```

### Configuration

Coming soon; no properties are exposed at this time, aside from the standard [Exec][4] interface.

[0]: https://docs.oracle.com/en/java/javase/11/tools/jmod.html
[1]: https://docs.oracle.com/en/java/javase/11/tools/jlink.html
[3]: https://docs.gradle.org/current/userguide/platforms.html#sub:central-declaration-of-dependencies
[4]: https://docs.gradle.org/current/dsl/org.gradle.api.tasks.Exec.html
