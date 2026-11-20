# Security model

This public repository contains the client action, not infrastructure credentials.
Pin `elide-dev/build-infra/actions/buildcache` to a reviewed full commit SHA. The gateway and its
Pulumi deployment remain in `elide-dev/cloud`; moving the client does not grant
the tools organization or this repository access to the cache.

## Job identity and trust

Grant `id-token: write` only to jobs using the cache. The action obtains a
short-lived GitHub token with audience `elide-build-cache`; it does not need a
BuildBuddy key or GCP credential. The gateway verifies the JWT signature, issuer,
audience, expiry, immutable owner/repository IDs, event and ref. It namespaces
HTTP cache indexes by repository, ref, protocol and format version. A branch or
PR can read its own entries and main's entries, and can write only its own.
These rules cover this HTTP adapter; Bazel's direct BuildBuddy API keys retain
their existing permissions.

Fork pull requests can carry the base repository's OIDC identity. Repository IDs
alone do not exclude forks. Consumer workflows select private runners and this
action only for trusted pushes, manual runs and same-repository pull requests;
their normal fork path uses hosted runners without private cache connectivity.
However, fork authors can edit workflow YAML, so workflow conditions alone are
not a sandbox for fork code.

The `elide-dev` organization and the public consumers require approval for **all
external contributors' fork runs**, including returning contributors. Before
approving, maintainers must review workflow changes and must not approve a fork
run that selects private runners or otherwise executes untrusted code inside the
fleet. Stronger unattended fork support requires a restricted immutable reusable
workflow or a separate isolated runner fleet. Do not use `pull_request_target`
or a privileged `workflow_run` to execute untrusted code. Maintainers who approve
private runner jobs are inside this trust boundary. Cache objects must never
contain secrets.

## Transport and process isolation

Clients connect to an authenticated relay bound only to `127.0.0.1`. Its random
job password is masked, state files are owner-only on POSIX runners (Windows
relies on the runner temp directory's ACLs; do not share a Windows runner
account between trust domains), and the child receives a minimal environment. Authentication is required for cache requests and shutdown.
The relay forwards only cache methods/paths to one configured origin; it never
follows redirects with the job token. It refreshes tokens before expiry, bounds
connection/idle waits and aborts upstream work when clients disconnect.

Use HTTPS outside the trusted VPC. Private HTTP is accepted only for literal
`10.0.0.0/8` addresses and loopback; it is **not application-level encrypted**.
Do not route it over a public network or an untrusted network attachment. The
endpoint is a trusted repository variable, never PR input. No direct CAS or
BuildBuddy credential is exposed through the HTTP relay.

Post-job cleanup removes the temporary Gradle init script and relay state and
requests authenticated shutdown. Runner teardown must still clean job processes
if a job is forcibly terminated before post steps execute.

## Repository controls

The action has no npm dependencies or generated dependency bundle. Tests run on
Linux and Windows with a read-only GitHub token and no cloud/OIDC permissions.
Workflow actions use full commit pins, checkout does not persist credentials,
and Linux jobs use `harden-runner`. Main requires CI and code-owner review and
disallows force pushes and deletion. Consumer pins are updated through PRs.

Report vulnerabilities privately to the repository maintainers; do not include
tokens or sensitive cache contents in a public issue.
