## Devops Services

Each sub-directory defines an API service handler which is used as part of Elide's internal Devops Services. These online (but private) services augment regular developer activities.

In addition to the services themselves, each implemented with a Cloudflare Worker, there are the [`commons`](./commons) and [`client`](./client) modules, where shared code is held and where the CLI client is defined, respectively.

### Available services

- **[`devstat`](./devstat)**: Reports stats in simple JSON payloads, which aggregate into Analytics Engine and D1; can then be used for charts in Grafana. "Stats" in this case include things like binary size, which powers PR comment diffs.

- **[`reports`](./reports)**: Accepts zipfiles full of HTML reports (or any other serveable static asset), which are registered in D1, stored in R2, and then made available via HTTPS; powers HTTP report access used on PRs.
