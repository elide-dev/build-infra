
## Gradle Baselines

Applies sensible Gradle project configuration baselines, as part of the other plug-ins provided by the `build-infra`
project. These include the following changes/settings:

- **Reproducibility for archives.** Archives (tar, zip, jar) are built with a reproducible file order, without file
  timestamps, and with Zip64, as applicable.

- **Dependency locking.** If configured via the extension DSL, dependency locking is enabled and applied; configurations
  matching a set of strings are ignored. The default operating mode is `LENIENT`. The default ignore-set will skip all
  configurations containing the term `detached` (ignorant of case and position).

- **Dependency verification.** If configured via the extension DSL, dependency verification is enabled and applied;
  configurations matching a set of strings are ignored. The default operating mode is `LENIENT`. The default ignore
  behavior is identical to _Dependency locking_.

### Disabling baselines

If you would prefer to skip the above settings, simply apply the following in your build:

```kotlin
buildInfra {
    baselines = false
}
```
