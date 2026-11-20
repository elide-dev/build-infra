# Elide build infrastructure

[![Shared actions and binstat](https://github.com/elide-dev/build-infra/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/elide-dev/build-infra/actions/workflows/ci.yml)
[![Actionlint](https://github.com/elide-dev/build-infra/actions/workflows/check.actions-lint.ci.yml/badge.svg?branch=main)](https://github.com/elide-dev/build-infra/actions/workflows/check.actions-lint.ci.yml)
[![License](https://img.shields.io/github/license/elide-dev/build-infra)](LICENSE)

Use the [action catalog](actions/README.md) for Elide Cloud login, cache and artifact leases, Google Cloud identity, Renovate,
and Bun/Turbo caching. The [shared build cache](actions/buildcache/README.md)
connects sccache and Gradle builds to Elide’s OIDC-authenticated Buildbarn cache.
Pin consumers to a full commit SHA. Each action documents its required permissions.

## Binary size reports

Elide Runtime uses the [devops CLI](services/client) to record binary sizes with
`binstats` and compare revisions with `bincompare`. The [stats worker](services/devstat)
persists the records. For the build-mode upgrade, apply the
[D1 migration](services/devstat/migrations/0001_build_mode.sql) once before deploying
the worker, then publish the client. Older records keep a null build mode.

## Development

With Node 24, pnpm 10.34.9 and Bun 1.4.4 installed:

```sh
pnpm install --frozen-lockfile
pnpm -r --if-present run type-check
bun test services
node --test actions/*/*.test.cjs
pnpm --dir services/client run build
```

Existing workflow callers can continue using the [JVM Gradle workflow](.github/workflows/jvm.gradle.yml).
Its optional provenance job uses GitHub artifact attestations and requires the caller to grant
`attestations: write` and `id-token: write`. The retired convention plugins, legacy image
builds, Bazel workflows and wrapper are available on the
[archive branch](https://github.com/elide-dev/build-infra/tree/archive/pre-shared-actions).
