# Shared GitHub Actions

Consumers pin each action to a reviewed commit in this repository. Replace
`COMMIT_SHA` with that full SHA when adopting or updating an action.

| Action | Caller setup |
| --- | --- |
| `elide-dev/build-infra/actions/elide-cloud@COMMIT_SHA` | Grant the job `id-token: write` and use its registered environment, branch and workflow. Exports `ELIDE_CLOUD_ACCESS_TOKEN` to subsequent steps. |
| `elide-dev/build-infra/actions/cloud-identity@COMMIT_SHA` | Check out the repo first. Grant the job `id-token: write` and pass the Google provider resource name and service account email. Subsequent steps use Application Default Credentials. Exclude `gha-creds-*.json` from artifacts and container build contexts. |
| `elide-dev/build-infra/actions/cache-build@COMMIT_SHA` | Check out a Bun/Turbo repo with a root bun.lock, install Bun and Node, then pass a stable job name as `scope`. Caches Bun downloads and Turbo outputs. |
| `elide-dev/build-infra/actions/renovate@COMMIT_SHA` | Run on a trusted schedule or manual dispatch on a Linux Docker runner. Targets the calling repository. Defaults to the job token. Grant contents and pull-requests write and enable Actions-created PRs. Pass a short-lived installation token as `token` when downstream PR workflows must run. |

## Google Cloud example

```yaml
permissions:
  contents: read
  id-token: write
steps:
  - uses: actions/checkout@9c091bb21b7c1c1d1991bb908d89e4e9dddfe3e0
    with:
      persist-credentials: false
  - uses: elide-dev/build-infra/actions/cloud-identity@COMMIT_SHA
    with:
      provider: ${{ vars.GOOGLE_WORKLOAD_IDENTITY_PROVIDER }}
      service-account: ${{ vars.GOOGLE_SERVICE_ACCOUNT }}
```

Configure trust in Google before running the job. Restrict the numeric owner
and repo IDs and the intended protected environment or ref. The action runs in
the calling job, so its path does not appear as a reusable workflow identity.
To inspect a consumer's claims, call the diagnostic workflow from a job:

```yaml
jobs:
  inspect:
    permissions:
      id-token: write
    uses: elide-dev/build-infra/.github/workflows/inspect-identity.yml@COMMIT_SHA
    with:
      environment: sandbox
```

The diagnostic prints selected claims for its own audience and workflow.
Use the actual production job's audience and workflow when configuring trust.

## Elide Cloud caches, artifacts and ledger

Run `actions/elide-cloud` in a registered protected environment. The CLI reads
the resulting account token for ledger operations. Grant `ledger:write` for
recording events and `ledger:read` if the job also reads ledger history.

For the Cloud build service, follow login with a lease. The job needs
`build:read` and, for uploads, `build:write`. This lease is organization-shared.
The project selects the storage namespace within the organization. The action
exports `ELIDE_CLOUD_BUILD_LEASE` for CLI cache and artifact commands and
revokes it at job teardown.

```yaml
- uses: elide-dev/build-infra/actions/elide-cloud@COMMIT_SHA
- uses: elide-dev/build-infra/actions/build-lease@COMMIT_SHA
  with:
    project: runtime
    writable: 'true'
    ttl-seconds: '900'
```
