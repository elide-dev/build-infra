
## Gradle Conventions: Plugins

This directory provides source code for the following Gradle plugins. For usage and other docs, see each plug-in
directory or the [Gradle build-infra README](..).

**Available Plugins**

- **[`dev.elide.base`][1]:** Just applies the baselines above, but can be switched off, too, to avoid interfering with
  your current build settings.

- **[`dev.elide.jmod`][2]:** Build [`jmod`][7] artifacts in Gradle JVM projects with Java 9+. `jmod` artifacts are
  compatible with [`jlink`][8] and make for great optimized build artifacts in modular projects.

- **[`dev.elide.jpms`][3]:** Toolkit plugin for Gradle builds enabled with modular Java (Java Platform Module System, or
  JPMS). Provides a `modulepath` configuration and modular builds for Java, Kotlin, and GraalVM.

- **[`dev.elide.mrjar`][4]:** Plugin for easily building multi-target MRJAR artifacts. This plugin goes above and beyond
  by **building the entire project at each bytecode tier**, so that modern Java runtimes can leverage the latest
  available bytecode for your app or library.

- **[`dev.elide.jlink`][5]:** Plugin for using `jmod` and `jpms` to build optimized, self-contained modular Java apps
  using [`jlink`][8].

- **[`dev.elide.graalvm`][6]:** Plugin for using `jmod` and `jpms` to build optimized, native AOT Java apps using
  [`native-image`][9].

- **[`dev.elide.gha`][10]:** Integrate your Gradle build with GitHub Actions. Enables enhanced logging and reporting
  features, PR integrations, and other features supported by the workflows in this repo.

[1]: ./base
[2]: ./jmod
[3]: ./jpms
[4]: ./mrjar
[5]: ./jlink
[6]: ./graalvm
[7]: https://www.oracle.com/corporate/features/understanding-java-9-modules.html
[8]: https://docs.oracle.com/en/java/javase/11/tools/jlink.html
[9]: https://www.graalvm.org/latest/reference-manual/native-image/
[10]: ./gha
