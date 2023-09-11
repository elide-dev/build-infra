# elide ci infra

This repository hosts CI configurations which can be re-used across repositories under the Elide Cloud umbrella. It isn't necessarily meant for external use, but it's open source and you are free to use these workflows in your own repos (no backward compatibility guarantee is provided at this time).

Build configurations expressed in this repository are typically used for repeated/templated repositories, such as code samples, plugins, and other structures which are consistent across codebases.

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

### Pushing updates

In general, Dependabot and Renovate are granted access to this repo, so that update PRs may be filed when internal actions
and workflows see updates.

## Usage example

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
| ---------------- | -------------------------------- |
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

| Name             | Type      | Description                      | Default value               |
| ---------------- | --------- | -------------------------------- | --------------------------- |
| `image`*         | `string`  | Image coordinate to build        | _(None. Required.)_         |
| `auth`           | `boolean` | Whether to authenticate          | `true`                      |
| `dockerfile`     | `string`  | Full path to Dockerfile to build | `"Dockerfile"`              |
| `path`           | `string`  | Docker context path for build    | `"."`                       |
| `platforms`      | `string`  | Architectures/platforms to build | `"linux/amd64,linux/arm64"` |
| `push`           | `boolean` | Whether to push after building   | `false`                     |
| `registry`       | `string`  | Whether to push after building   | `"ghcr.io"`                 |
| `runner`         | `string`  | Runner to use for all tasks      | _(See runner docs)_         |
| `tags`           | `string`  | Tags to push to with built image | _(None.)_                   |

### JVM: Gradle

- **Description:** Consistently build JVM outputs using Gradle
- **Workflow:** `.github/workflows/jvm.gradle.yml`

#### Inputs

| Name              | Type      | Description                      | Default value               |
| ----------------- | --------- | -------------------------------- | --------------------------- |
| `action`          | `string`  | Gradle task(s) to execute        | `"build"`                   |
| `artifact`        | `string`  | Name of output artifact to use   | _(None.)_                   |
| `artifacts`       | `boolean` | Upload built artifacts           | `false`                     |
| `cache_action`    | `boolean` | Turn GHA cache on/off            | `true`                      |
| `cache_local`     | `boolean` | Turn local caching on/off        | `false`                     |
| `cache_read_only` | `boolean` | GHA cache read-only status       | `false`                     |
| `cache_remote`    | `boolean` | Turn remote caching on/off       | `true`                      |
| `checks`          | `boolean` | Run checks and Sonar             | `true`                      |
| `coverage`        | `boolean` | Upload → Codecov after build     | `false`                     |
| `coverage_report` | `string`  | Path to coverage report          | _(None.)_                   |
| `coverage_flags`  | `string`  | Extra flags to pass to Codecov   | _(None.)_                   |
| `flags`           | `string`  | Extra flags to append            | _(None.)_                   |
| `gradle`          | `string`  | Gradle version to install & use  | `"wrapper"`                 |
| `gvm`             | `string`  | GraalVM version to use           | _(See JVM notes below)_     |
| `gvm_components`  | `string`  | GraalVM components to install    | `"native-image,js"`         |
| `install_gvm`     | `boolean` | Setup a distribution of GraalVM  | `false`                     |
| `install_jvm`     | `boolean` | Setup a regular JVM before build | `true`                      |
| `jvm`             | `string`  | JVM version to install/target    | _(See JVM notes below)_     |
| `jvm_dist`        | `string`  | JVM distribution to use          | `"adopt-hotspot"`           |
| `label`           | `string`  | Label to show for build step     | `"Gradle"`                  |
| `provenance`      | `boolean` | Stamp for SLSA provenance        | `false`                     |
| `publish`         | `boolean` | Perform a publish after build    | `false`                     |
| `reports`         | `boolean` | Whether to upload built reports  | `true`                      |
| `runner`          | `string`  | Runner to use for all tasks      | _(See runner docs)_         |

### JVM: Maven

- **Description:** Consistently build JVM outputs using Maven
- **Workflow:** `.github/workflows/jvm.maven.yml`

#### Inputs

Inputs for the Maven workflow are nearly identical to those for the Gradle workflow (listed above):

| Name              | Type      | Description                      | Default value               |
| ----------------- | --------- | -------------------------------- | --------------------------- |
| `action`          | `string`  | Maven goal(s) to execute         | `"package"`                 |
| `artifact`        | `string`  | Name of output artifact to use   | _(None.)_                   |
| `artifacts`       | `boolean` | Upload built artifacts           | `false`                     |
| `cache_action`    | `boolean` | Turn GHA cache on/off            | `true`                      |
| `cache_local`     | `boolean` | Turn local caching on/off        | `false`                     |
| `cache_read_only` | `boolean` | GHA cache read-only status       | `false`                     |
| `cache_remote`    | `boolean` | Turn remote caching on/off       | `true`                      |
| `checks`          | `boolean` | Run checks and Sonar             | `true`                      |
| `coverage`        | `boolean` | Upload → Codecov after build     | `false`                     |
| `coverage_report` | `string`  | Path to coverage report          | _(None.)_                   |
| `coverage_flags`  | `string`  | Extra flags to pass to Codecov   | _(None.)_                   |
| `flags`           | `string`  | Extra flags to append            | _(None.)_                   |
| `gvm`             | `string`  | GraalVM version to use           | _(See JVM notes below)_     |
| `gvm_components`  | `string`  | GraalVM components to install    | `"native-image,js"`         |
| `install_gvm`     | `boolean` | Setup a distribution of GraalVM  | `false`                     |
| `install_jvm`     | `boolean` | Setup a regular JVM before build | `true`                      |
| `jvm`             | `string`  | JVM version to install/target    | _(See JVM notes below)_     |
| `jvm_dist`        | `string`  | JVM distribution to use          | `"adopt-hotspot"`           |
| `label`           | `string`  | Label to show for build step     | `"Gradle"`                  |
| `provenance`      | `boolean` | Stamp for SLSA provenance        | `false`                     |
| `publish`         | `boolean` | Perform a publish after build    | `false`                     |
| `reports`         | `boolean` | Whether to upload built reports  | `true`                      |
| `runner`          | `string`  | Runner to use for all tasks      | _(See runner docs)_         |

### Bazel

- **Description:** Run builds with Bazel
- **Workflow:** `.github/workflows/bazel.yml`

#### Inputs

There are no required inputs for a Bazel build; the target specification defaults to the value
`//...`, which builds all targets. The default `command` is `build`. The build is executed with
Bazelisk, which will respect the `.bazelversion` present at the root of your project.

| Name              | Type      | Description                      | Default value               |
| ----------------- | --------- | -------------------------------- | --------------------------- |
| `artifact`        | `string`  | Name of output artifact to use   | _(None.)_                   |
| `artifacts`       | `boolean` | Upload built artifacts           | `false`                     |
| `cache_action`    | `boolean` | Turn GHA cache on/off            | `true`                      |
| `command`         | `string`  | Bazel command to execute         | `"build"`                   |
| `targets`         | `string`  | Bazel target string              | `"//..."`                   |
| `flags`           | `string`  | Extra flags to append            | _(None.)_                   |
| `gvm`             | `string`  | GraalVM version to use           | _(See JVM notes below)_     |
| `gvm_components`  | `string`  | GraalVM components to install    | `"native-image,js"`         |
| `install_gvm`     | `boolean` | Setup a distribution of GraalVM  | `false`                     |
| `install_jvm`     | `boolean` | Setup a regular JVM before build | `true`                      |
| `jvm`             | `string`  | JVM version to install/target    | _(See JVM notes below)_     |
| `jvm_dist`        | `string`  | JVM distribution to use          | `"adopt-hotspot"`           |
| `runner`          | `string`  | Runner to use for all tasks      | _(See runner docs)_         |

## Contributing

Follow the YAML and action convention within the repository; the multi-workspace expressed here for actions uses the
[Actions Toolkit](https://github.com/actions/toolkit) and [`pnpm`](https://pnpm.io/) [workspaces](https://pnpm.io/workspaces).

Build configurations are validated on each push, and actions are built and tested to the extent possible. Once a PR is merged,
it is expected to be deployed to private package storage, where update tools can pick it up.

## License

Buildless, Elide Cloud, and related code, is privately licensed, and this repository is not meant for public consumption.
Access implies that you accept the terms of the Elide Ventures LLC _Non-Disclosure Agreement_. All rights are reserved by
Elide Ventures, LLC.
