# elide ci infra

[![Lint: Actions](https://github.com/elide-dev/build-infra/actions/workflows/check.actions-lint.ci.yml/badge.svg)](https://github.com/elide-dev/build-infra/actions/workflows/check.actions-lint.ci.yml)
[![Lint: YAML](https://github.com/elide-dev/build-infra/actions/workflows/check.yaml-lint.ci.yml/badge.svg)](https://github.com/elide-dev/build-infra/actions/workflows/check.yaml-lint.ci.yml)
[![OpenSSF Best Practices](https://www.bestpractices.dev/projects/7693/badge)](https://www.bestpractices.dev/projects/7693)

### Structure

Each workflow profile is defined in this repository as a GitHub Workflow, using YAML. In some cases, workflows are paired
with a container image which includes the tools which are used by the workflow. The image typically isn't required (unless
noted) but may vastly speed up builds by pre-including tools.

For an exhaustive list of build profiles and their use, see the table further down.

- **[Containers](./containers): Container image definitions** which are used by **actions** and **workflows**; some of
  these can also be used directly in downstream workflows.

- **[Actions](./actions): Custom GitHub Actions** for use in Elide codebases. Actions are used like any other GitHub Action,
  but with references to this repository instead.

- **[Workflows](./.github/workflows/pkg): Custom YAML workflow definitions** for use in Elide codebases. Workflows are used as external
  `workflow_call` targets.

- **[Gradle](./gradle): Gradle build conventions** which are re-usable across projects, as a suite of easily applicable build convention
  plugins and version catalogs.

## Workflows

In a `.github/workflows/<x>.yml`:

```yaml
# ...

jobs:
  ## Build container
  build-a-container:
    name: "Image"
    uses: elide-dev/build-infra/.github/workflows/container.yml@main
    secrets: inherit
    permissions:
      checks: write
      id-token: write
      contents: read
      packages: write
      pull-requests: write
    with:
      image: elide-dev/build-infra/gvm
      path: containers/gvm
      push: ${{ github.event_name == 'push' && github.ref == 'refs/heads/main' }}
```

The above job uses the `container.yml` "build profile" (see all profiles listed below). By using the re-usable workflow, you
gain consistency:

- Repository authentication is handled for you
- Intelligent platform selection (with override)
- Consistent tagging and labeling of images
- Underlying Github Actions updates happen without repo commits


# Build profiles

| Name             | Description                      |
|------------------|----------------------------------|
| `android.yml`    | Build an Android app with Gradle |
| `container.yml`  | Build and push a container image |
| `jvm.gradle.yml` | Run a Gradle build targeting JVM |
| `jvm.maven.yml`  | Run a Maven build targeting JVM  |
| `bazel.yml`      | Build targets with Bazel         |

See below for documentation about reusable workflow inputs.

## Workflow inputs

See below for an exhaustive list of all inputs for each build profile. You can use these inputs in the `with: {}` block of your workflow invocation.

### Containers

- **Description:** Consistently build properly tagged container images in sync with source control
- **Workflow:** `.github/workflows/container.yml`

#### Inputs

| Name         | Type      | Description                      | Default value               |
|--------------|-----------|----------------------------------|-----------------------------|
| `image`*     | `string`  | Image coordinate to build        | _(None. Required.)_         |
| `auth`       | `boolean` | Whether to authenticate          | `true`                      |
| `dockerfile` | `string`  | Full path to Dockerfile to build | `"Dockerfile"`              |
| `path`       | `string`  | Docker context path for build    | `"."`                       |
| `platforms`  | `string`  | Architectures/platforms to build | `"linux/amd64,linux/arm64"` |
| `push`       | `boolean` | Whether to push after building   | `false`                     |
| `registry`   | `string`  | Whether to push after building   | `"ghcr.io"`                 |
| `runner`     | `string`  | Runner to use for all tasks      | _(See runner docs)_         |
| `tags`       | `string`  | Tags to push to with built image | _(None.)_                   |

### Gradle: Android

- **Description:** Consistently build JVM outputs using Gradle
- **Workflow:** `.github/workflows/jvm.gradle.yml`

#### Inputs

| Name              | Type      | Description                      | Default value           |
|-------------------|-----------|----------------------------------|-------------------------|
| `action`          | `string`  | Gradle task(s) to execute        | `"build"`               |
| `android`         | `boolean` | Install Android SDK              | true                    |
| `artifact`        | `string`  | Name of output artifact to use   | _(None.)_               |
| `artifacts`       | `boolean` | Upload built artifacts           | `false`                 |
| `cache_action`    | `boolean` | Turn GHA cache on/off            | `true`                  |
| `cache_local`     | `boolean` | Turn local caching on/off        | `false`                 |
| `cache_read_only` | `boolean` | GHA cache read-only status       | `false`                 |
| `cache_remote`    | `boolean` | Turn remote caching on/off       | `true`                  |
| `checks`          | `boolean` | Run checks and Sonar             | `true`                  |
| `coverage`        | `boolean` | Upload → Codecov after build     | `false`                 |
| `coverage_report` | `string`  | Path to coverage report          | _(None.)_               |
| `coverage_flags`  | `string`  | Extra flags to pass to Codecov   | _(None.)_               |
| `flags`           | `string`  | Extra flags to append            | _(None.)_               |
| `gradle`          | `string`  | Gradle version to install & use  | `"wrapper"`             |
| `install_jvm`     | `boolean` | Setup a regular JVM before build | `true`                  |
| `jvm`             | `string`  | JVM version to install/target    | _(See JVM notes below)_ |
| `jvm_dist`        | `string`  | JVM distribution to use          | `"adopt-hotspot"`       |
| `label`           | `string`  | Label to show for build step     | `"Gradle"`              |
| `reports`         | `boolean` | Whether to upload built reports  | `true`                  |
| `runner`          | `string`  | Runner to use for all tasks      | _(See runner docs)_     |


### Gradle: JVM

- **Description:** Consistently build JVM outputs using Gradle
- **Workflow:** `.github/workflows/jvm.gradle.yml`

#### Inputs

| Name              | Type      | Description                      | Default value           |
|-------------------|-----------|----------------------------------|-------------------------|
| `action`          | `string`  | Gradle task(s) to execute        | `"build"`               |
| `artifact`        | `string`  | Name of output artifact to use   | _(None.)_               |
| `artifacts`       | `boolean` | Upload built artifacts           | `false`                 |
| `cache_action`    | `boolean` | Turn GHA cache on/off            | `true`                  |
| `cache_local`     | `boolean` | Turn local caching on/off        | `false`                 |
| `cache_read_only` | `boolean` | GHA cache read-only status       | `false`                 |
| `cache_remote`    | `boolean` | Turn remote caching on/off       | `true`                  |
| `checks`          | `boolean` | Run checks and Sonar             | `true`                  |
| `coverage`        | `boolean` | Upload → Codecov after build     | `false`                 |
| `coverage_report` | `string`  | Path to coverage report          | _(None.)_               |
| `coverage_flags`  | `string`  | Extra flags to pass to Codecov   | _(None.)_               |
| `flags`           | `string`  | Extra flags to append            | _(None.)_               |
| `gradle`          | `string`  | Gradle version to install & use  | `"wrapper"`             |
| `gvm`             | `string`  | GraalVM version to use           | _(See JVM notes below)_ |
| `gvm_components`  | `string`  | GraalVM components to install    | `"native-image,js"`     |
| `install_gvm`     | `boolean` | Setup a distribution of GraalVM  | `false`                 |
| `install_jvm`     | `boolean` | Setup a regular JVM before build | `true`                  |
| `jvm`             | `string`  | JVM version to install/target    | _(See JVM notes below)_ |
| `jvm_dist`        | `string`  | JVM distribution to use          | `"adopt-hotspot"`       |
| `label`           | `string`  | Label to show for build step     | `"Gradle"`              |
| `provenance`      | `boolean` | Stamp for SLSA provenance        | `false`                 |
| `publish`         | `boolean` | Perform a publish after build    | `false`                 |
| `reports`         | `boolean` | Whether to upload built reports  | `true`                  |
| `runner`          | `string`  | Runner to use for all tasks      | _(See runner docs)_     |

### JVM: Maven

- **Description:** Consistently build JVM outputs using Maven
- **Workflow:** `.github/workflows/jvm.maven.yml`

#### Inputs

Inputs for the Maven workflow are nearly identical to those for the Gradle workflow (listed above):

| Name              | Type      | Description                      | Default value           |
|-------------------|-----------|----------------------------------|-------------------------|
| `action`          | `string`  | Maven goal(s) to execute         | `"package"`             |
| `artifact`        | `string`  | Name of output artifact to use   | _(None.)_               |
| `artifacts`       | `boolean` | Upload built artifacts           | `false`                 |
| `cache_action`    | `boolean` | Turn GHA cache on/off            | `true`                  |
| `cache_local`     | `boolean` | Turn local caching on/off        | `false`                 |
| `cache_read_only` | `boolean` | GHA cache read-only status       | `false`                 |
| `cache_remote`    | `boolean` | Turn remote caching on/off       | `true`                  |
| `checks`          | `boolean` | Run checks and Sonar             | `true`                  |
| `coverage`        | `boolean` | Upload → Codecov after build     | `false`                 |
| `coverage_report` | `string`  | Path to coverage report          | _(None.)_               |
| `coverage_flags`  | `string`  | Extra flags to pass to Codecov   | _(None.)_               |
| `flags`           | `string`  | Extra flags to append            | _(None.)_               |
| `gvm`             | `string`  | GraalVM version to use           | _(See JVM notes below)_ |
| `gvm_components`  | `string`  | GraalVM components to install    | `"native-image,js"`     |
| `install_gvm`     | `boolean` | Setup a distribution of GraalVM  | `false`                 |
| `install_jvm`     | `boolean` | Setup a regular JVM before build | `true`                  |
| `jvm`             | `string`  | JVM version to install/target    | _(See JVM notes below)_ |
| `jvm_dist`        | `string`  | JVM distribution to use          | `"adopt-hotspot"`       |
| `label`           | `string`  | Label to show for build step     | `"Gradle"`              |
| `provenance`      | `boolean` | Stamp for SLSA provenance        | `false`                 |
| `publish`         | `boolean` | Perform a publish after build    | `false`                 |
| `reports`         | `boolean` | Whether to upload built reports  | `true`                  |
| `runner`          | `string`  | Runner to use for all tasks      | _(See runner docs)_     |

### Bazel

- **Description:** Run builds with Bazel
- **Workflow:** `.github/workflows/bazel.yml`

#### Inputs

There are no required inputs for a Bazel build; the target specification defaults to the value
`//...`, which builds all targets. The default `command` is `build`. The build is executed with
Bazelisk, which will respect the `.bazelversion` present at the root of your project.

| Name             | Type      | Description                      | Default value           |
|------------------|-----------|----------------------------------|-------------------------|
| `artifact`       | `string`  | Name of output artifact to use   | _(None.)_               |
| `artifacts`      | `boolean` | Upload built artifacts           | `false`                 |
| `cache_action`   | `boolean` | Turn GHA cache on/off            | `true`                  |
| `command`        | `string`  | Bazel command to execute         | `"build"`               |
| `targets`        | `string`  | Bazel target string              | `"//..."`               |
| `flags`          | `string`  | Extra flags to append            | _(None.)_               |
| `gvm`            | `string`  | GraalVM version to use           | _(See JVM notes below)_ |
| `gvm_components` | `string`  | GraalVM components to install    | `"native-image,js"`     |
| `install_gvm`    | `boolean` | Setup a distribution of GraalVM  | `false`                 |
| `install_jvm`    | `boolean` | Setup a regular JVM before build | `true`                  |
| `jvm`            | `string`  | JVM version to install/target    | _(See JVM notes below)_ |
| `jvm_dist`       | `string`  | JVM distribution to use          | `"adopt-hotspot"`       |
| `runner`         | `string`  | Runner to use for all tasks      | _(See runner docs)_     |

## Gradle Conventions

The Gradle conventions provided by this project are generic in nature and can be used in nearly any Gradle 8+ project.
Conventions are applied in a cascading fashion, with relevant plugins being applied first, which then provide strong
baseline settings.

**Gradle infra:**

- **[Plugins](./gradle/plugins)** for common build tasks, especially around [JPMS][8]. See the plugins list below.
- **[Catalogs](./gradle/catalogs)** defining security-hardened library catalogs
- **[Platforms](./gradle/platforms)** which enforce different profiles of dependency constraints

Read more about the Gradle build infra [here](./gradle). There are [samples](./gradle/samples).

### Plugins

- **[`dev.elide.gha`][9]:** Integrate your Gradle build with GitHub Actions. Enables enhanced logging and reporting
  features, PR integrations, and other features supported by the workflows in this repo.

- **[`dev.elide.jmod`][0]:** Build [`jmod`][1] artifacts in Gradle JVM projects with Java 9+. `jmod` artifacts are
  compatible with [`jlink`][2] and make for great optimized build artifacts in modular projects.

- **[`dev.elide.jpms`][3]:** Toolkit plugin for Gradle builds enabled with modular Java (Java Platform Module System, or
  JPMS). Provides a `modulepath` configuration and modular builds for Java, Kotlin, and GraalVM.

- **[`dev.elide.mrjar`][4]:** Plugin for easily building multi-target MRJAR artifacts. This plugin goes above and beyond
  by building the entire project at each bytecode tier, so that modern Java runtimes can leverage the latest bytecode.

- **[`dev.elide.jlink`][5]:** Plugin for using `jmod` and `jpms` to build optimized, self-contained modular Java apps
  using [`jlink`][2].

- **[`dev.elide.graalvm`][6]:** Plugin for using `jmod` and `jpms` to build optimized, native AOT Java apps using
  [`native-image`][7].

## Contributing

Follow the YAML and action convention within the repository; the multi-workspace expressed here for actions uses the
[GitHub Actions Toolkit](https://github.com/actions/toolkit) and [`pnpm`](https://pnpm.io/) [workspaces](https://pnpm.io/workspaces).

Build configurations are validated on each push, and actions are built and tested to the extent possible. Once a PR is merged,
it is expected to be deployed to private package storage, where update tools can pick it up.

## License

This repository is shared openly for OSS use. It is licensed as MIT.

[0]: ./gradle/plugins/jmod
[1]: https://docs.oracle.com/en/java/javase/11/tools/jmod.html
[2]: https://docs.oracle.com/en/java/javase/11/tools/jlink.html
[3]: ./gradle/plugins/jpms
[4]: ./gradle/plugins/mrjar
[5]: ./gradle/plugins/jlink
[6]: ./gradle/plugins/graalvm
[7]: https://www.graalvm.org/latest/reference-manual/native-image/
[8]: https://www.oracle.com/corporate/features/understanding-java-9-modules.html
[9]: ./gradle/plugins/gha
