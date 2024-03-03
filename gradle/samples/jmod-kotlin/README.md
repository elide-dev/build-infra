
## Sample: Kotlin with `jmod`

This project builds a Kotlin/JVM project with [`jmod`][0], using the [Build Infra JMod Plugin](../../plugins/jmod).
Usage is simple:

**In your `build.gradle.kts`**:
```kotlin
plugins {
  java
  kotlin("multiplatform")
  id("dev.elide.jmod")
}

kotlin {
  jvm {
    withJava()
  }
}
```

**In `src/main/java/module-info.java`:**
```java
module my.cool.module {
  requires java.base;
  requires kotlin.stdlib;

  // ...
}
```

**Then, you can do:**
```
./gradlew jmod
```

**Which gives you:**
```
build/jmod/<project>.jmod
```

> [!IMPORTANT]
> You need a `module-info.java` definition to build a `jmod`.

## Why would anyone want to build these?

You can use `jmod` artifacts downstream with tools like [`jlink`][1]; in particular, this is possible in Gradle through
the [Build Infra JLink Plugin](../../plugins/jlink).

[0]: https://docs.oracle.com/en/java/javase/11/tools/jmod.html
[1]: https://docs.oracle.com/en/java/javase/11/tools/jlink.html
