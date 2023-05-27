# elide ci infra

This repository hosts CI configurations which can be re-used across private repositories under the Elide Cloud umbrella.
Build configurations expressed in this repository are typically used for repeated/templated repositories, such as code
samples, plugins, and other structures which are consistent across codebases.

### Structure

Each workflow profile is defined in this repository as a GitHub Workflow, using YAML. In some cases, workflows are paired
with a container image which includes the tools which are used by the workflow. The image typically isn't required (unless
noted) but may vastly speed up builds by pre-including tools.

For an exhaustive list of build profiles and their use, see the table further down.

- **[Containers](./containers): Container image definitions** which are used by **actions** and **workflows**; some of
  these can also be used directly in downstream workflows.

- **[Actions](./actions): Custom GitHub Actions** for use in Elide codebases. Actions are used like any other GitHub Action,
  but with references to this repository instead.

- **[Workflows](./workflows): Custom YAML workflow definitions** for use in Elide codebases. Workflows are used as external
  `workflow_call` targets.

### Pushing updates

In general, Dependabot and Renovate are granted access to this repo, so that update PRs may be filed when internal actions
and workflows see updates.

## Build profiles

Coming soon.

## Contributing

Follow the YAML and action convention within the repository; the multi-workspace expressed here for actions uses the
[Actions Toolkit](https://github.com/actions/toolkit) and [`pnpm`](https://pnpm.io/) [workspaces](https://pnpm.io/workspaces).

Build configurations are validated on each push, and actions are built and tested to the extent possible. Once a PR is merged,
it is expected to be deployed to private package storage, where update tools can pick it up.

## License

Buildless, Elide Cloud, and related code, is privately licensed, and this repository is not meant for public consumption.
Access implies that you accept the terms of the Elide Ventures LLC _Non-Disclosure Agreement_. All rights are reserved by
Elide Ventures, LLC.
