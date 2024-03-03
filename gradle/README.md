
## Gradle Build Infra

This subdirectory provides shared Gradle build conventions, which are designed for a hardened security posture and high-
performance build experience on modern JVM toolchains. This framework of convention plugins (and other artifacts) runs
the gamut in terms of project applicability, but has a particular focus for JVM applications and library authors,
especially with Kotlin.

### What's so great about these?

Plugins are provided for integrating various Java ecosystem features with Gradle, especially around [JPMS][0]. There are
plugins which also set sensible baseline defaults, including:

- **Better reproducibility** of archives (zip, tar, jar), which improves cacheability of outputs
- **Sensible defaults for dependency security**, including [dependency locking][1] and [verification][2]
- **Strong configuration** for linting and code checking, with unopinionated configuration control
- **Testing features** like test logging and multi-module merged test + coverage reporting out of the box

> [!TIP]
> 👆 These are the features included with use of any plugin provided here.

### Available Plugins

On top of the above functionality, there are feature plugins:

- **[`dev.elide.base`][11]:** Just applies the baselines above, but can be switched off, too, to avoid interfering with
  your current build settings.

- **[`dev.elide.jmod`][5]:** Build [`jmod`][3] artifacts in Gradle JVM projects with Java 9+. `jmod` artifacts are
  compatible with [`jlink`][4] and make for great optimized build artifacts in modular projects.

- **[`dev.elide.jpms`][6]:** Toolkit plugin for Gradle builds enabled with modular Java (Java Platform Module System, or
  JPMS). Provides a `modulepath` configuration and modular builds for Java, Kotlin, and GraalVM.

- **[`dev.elide.mrjar`][7]:** Plugin for easily building multi-target MRJAR artifacts. This plugin goes above and beyond
  by **building the entire project at each bytecode tier**, so that modern Java runtimes can leverage the latest
  available bytecode for your app or library.

- **[`dev.elide.jlink`][8]:** Plugin for using `jmod` and `jpms` to build optimized, self-contained modular Java apps
  using [`jlink`][4].

- **[`dev.elide.graalvm`][9]:** Plugin for using `jmod` and `jpms` to build optimized, native AOT Java apps using
  [`native-image`][10].

[0]: https://www.oracle.com/corporate/features/understanding-java-9-modules.html
[1]: https://docs.gradle.org/current/userguide/dependency_locking.html
[2]: https://docs.gradle.org/current/userguide/dependency_verification.html
[3]: https://docs.oracle.com/en/java/javase/11/tools/jmod.html
[4]: https://docs.oracle.com/en/java/javase/11/tools/jlink.html
[5]: ./plugins/jmod
[6]: ./plugins/jpms
[7]: ./plugins/mrjar
[8]: ./plugins/jlink
[9]: ./plugins/graalvm
[10]: https://www.graalvm.org/latest/reference-manual/native-image/
[11]: ./plugins/base
