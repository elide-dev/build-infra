
## Gradle Conventions: Supplemental Libraries

Provides supplemental **Maven libraries**, and **[version catalogs][0]**, for the `build-infra` project. These libraries
and catalogs are used under the hood by these plugins, and can be re-used for other projects.

### Why ship some catalogs?

Firstly, we want to align our own versions internally; but secondly, there are often _very sensible_ constraints that
can be placed on JVM builds in order to **harden security** and **improve build performance**. These catalogs define the
latest versions of tools like [Kotlin][1], [Guava][2], [Apache Commons][3], and more, which are used most often in
projects targeting JVM.

If you are using Gradle, Version Catalogs are a good alternative to [Maven BOMs][4], because:

- You can pick from them like a menu; only the dependencies you pick are downloaded
- You can access the version string, dependency coordinates, and even bundles of like dependencies
- Gradle generates accessors for you, that look like this: `libs.guava`, or `libs.kotlin.stdlib`
- Maven BOMs will often cause all sorts of unrelated metadata crawling; version catalogs don't have this problem

### Available Catalogs

There are three available catalogs: `core`, `infra`, and `libs`. Each targets a different use case and level of
opinionation.

#### `core`

The `core` catalog is unopinionated and focuses on critical JVM tooling, like **Kotlin**, **IDEA**, and plugins from the
Gradle team. Plugins are declared both as **plugins** and **libraries**, allowing these artifacts to be used in
`buildSrc` or `build-logic` projects as well as regular Gradle projects.

**The `core` catalog contains tools which are:**

1) Used by the `build-infra` project itself
2) Vetted for dependency security and liveness
3) Very un-opinionated, other than declaring the latest stable versions

#### `infra`

The `infra` catalog focuses on build tooling with a particular focus on the Gradle plug-in ecosystem. The best and most
popular Gradle plugins are declared with their latest versions, as both **plugins** and **libraries**, allowing these
artifacts to be used in `buildSrc` or `build-logic`-style projects as well as regular Gradle projects.

**The `infra` catalog contains tools which are:**

1) Used for builds, either by `build-infra` or downstream consumers
2) Vetted for dependency security and liveness
3) Slightly opinionated, in the sense that some plugins and features won't apply to some users

#### `libs`

The `libs` catalog focuses on compile-time and run-time libraries which might be useful within the broader JVM
ecosystem. This includes things like **Guava**, **Apache Commons**, compression libraries, and so on. Plugins provided
in this catalog focus on cosmetic/optional build improvements.

**The `libs` catalog contains libraries which are:**

1) Used anywhere, generally, for a lot of things
2) Vetted for dependency security and liveness
3) Strongly opinionated, in the sense that the latest stable version is always declared

[0]: https://docs.gradle.org/current/userguide/platforms.html#sec:sharing-catalogs
[1]: https://kotlinlang.org
[2]: https://github.com/google/guava
[3]: https://commons.apache.org/
[4]: https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#bill-of-materials-bom-poms
