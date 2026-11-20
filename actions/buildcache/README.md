# Shared build cache

A GitHub Action that connects sccache (WebDAV) and Gradle (HTTP build cache) to a
private shared cache, authenticated with the job's GitHub OIDC identity. No cloud
or cache key is distributed to the job.

The action does not install sccache, Gradle, a compiler or a JDK.

## Usage

```yaml
permissions:
  contents: read
  id-token: write
steps:
  - uses: elide-dev/build-infra/actions/buildcache@<commit-sha>
    with:
      endpoint: ${{ vars.BUILD_CACHE_ENDPOINT }}
```

- Pin the action to a reviewed full commit SHA.
- Run it in a trusted job, on a runner with network access to the cache.
- Keep fork pull requests on hosted runners without this step. See
  [SECURITY.md](SECURITY.md) for the full trust model.

### Inputs

| Input | Default | Description |
|---|---|---|
| `endpoint` | required | Cache origin. HTTPS, or HTTP on a literal `10.0.0.0/8` or loopback address. |
| `namespace` | `v1` | Version namespace for cache keys within a repository and ref. |
| `gradle` | `true` | Install a temporary Gradle init script. `false` skips it. |
| `required` | `false` | Fail the job if the cache or GitHub OIDC is unavailable. |

### Outputs

| Output | Description |
|---|---|
| `enabled` | `true` once the cache accepted this job's identity, otherwise `false`. |
| `endpoint` | URL of the job-local authenticated relay. |

## How it works

The action starts a relay bound to `127.0.0.1` and protected by a random per-job
password. The relay attaches a short-lived GitHub OIDC token (audience
`elide-build-cache`) to each request, refreshes it before expiry, and forwards
only cache methods and paths to the configured origin. It never follows
redirects. A post-job step shuts the relay down and removes its state and the
Gradle init script.

The cache gateway verifies the token's signature, issuer, audience, expiry,
owner and repository IDs, event and ref. Each repository and ref has its own
cache index: branches and pull requests can read their own entries and `main`'s,
and can write only their own.

By default, setup failure leaves the job's existing cache configuration
untouched. `ELIDE_BUILD_CACHE_ENABLED=true` is exported only after the gateway
has accepted the job's identity. A cache outage later in the job follows each
client's normal remote-cache failure behavior.

## Gradle

The init script adds a remote `HttpBuildCache` with push enabled and sets
`-Dorg.gradle.caching=true` in `GRADLE_OPTS`. The existing local-cache policy,
`GRADLE_USER_HOME` and credentials are preserved; the script reads only job
environment variables.

## sccache

The action exports `SCCACHE_WEBDAV_ENDPOINT`, `SCCACHE_WEBDAV_USERNAME`,
`SCCACHE_WEBDAV_PASSWORD`, `SCCACHE_WEBDAV_KEY_PREFIX` and
`SCCACHE_WEBDAV_RW_MODE=READ_WRITE`.

- Use a WebDAV-enabled sccache release. CI exercises sccache 0.14.0.
- Set `RUSTC_WRAPPER=sccache` or your compiler launcher yourself.
- Stop any running sccache server before this step so it picks up the backend.
- Remove competing backends such as `SCCACHE_GHA_ENABLED`; the action does not
  override them.

The relay forwards depth-zero `PROPFIND` and `MKCOL` for sccache's directory
checks. Recursive listing and `DELETE` are rejected.

## Adopting alongside existing caches

- Replace other remote output caches: disable the sccache GHA backend and
  GitHub Actions archives of Gradle `build-cache-*` output on jobs using this
  action.
- Keep dependency caches (npm/Bun, Maven, Go modules), toolchain installs and
  workflow artifacts. They serve a different purpose.
- Keep local caches for latency and for jobs that cannot reach this service.

## Durability

Objects keep their native sccache/Gradle compression; the relay does not
recompress them. A `PUT` is acknowledged only after the backing store has
committed the object and its index entry, so acknowledged objects survive loss
of a cache node's local disk, subject to normal eviction.

## Development

```sh
node --test relay.test.cjs sccache.test.cjs
```

The action has no npm dependencies. The real-sccache test runs when
`SCCACHE_TEST_BINARY` points at an sccache binary.
