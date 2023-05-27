
## elide ci: containers

This directory provides container definitions (one per sub-directory), for container images which are used
within CI/CD build pipelines internally at Elide.

Most of the time, you shouldn't need to interact with these container images directly; they are used by
default from [Actions](../actions) and [Workflows](../workflows). You can read more about this repository's
structure in the [main README](../).

### Containers provided by this repository

- **[`gvm`](./gvm)**: GraalVM latest, running JDK 19. Pre-installed `js` and `native-image`
  components.

- **[`jvm`](./jvm)**: HotSpot JVM latest, running JDK 19.
