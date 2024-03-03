
## Gradle JPMS Plugin

This plugin provides support and utilities for integrating Gradle with the [Java Platform Module System][0], also known
as _Project Jigsaw_ or JPMS.

On its own, this plugin doesn't do much. It is used by other plugins to share configurations and base functionality
which is needed for modular Java builds. Read on below to learn more about what this plug-in does.

### How to use it

```kotlin
plugin {
    id("dev.elide.jpms")
}

dependencies {
    modulepath("your.cool:dependency")
}
```

### What it does

- **`modulepath` configuration**: This plug-in adds a Gradle [Configuration][1] called `modulepath`, which is used to
  specify modular dependencies. These dependencies factor into `jmod` and `jlink` builds, and automatically inject into
  the counterpart `classpath` configuration.

### What uses it

The [`jmod` plugin][2], [`jlink` plugin][3], [`graalvm` plugin][4], and [`jlink` plugin][5] all use this one to manage
basic JPMS configuration.

[0]: https://www.oracle.com/corporate/features/understanding-java-9-modules.html
[1]: https://docs.gradle.org/current/dsl/org.gradle.api.artifacts.Configuration.html
[2]: ../jmod
[3]: ../jlink
[4]: ../graalvm
[5]: ../jlink
