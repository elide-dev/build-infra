
/**
 *
 * Elide Build Infrastructure for Gradle
 * -------------------------------------
 *
 * This script is designed to be applied directly to a Gradle build, in the form of a `apply(from = ...)`
 * directive within a `settings.gradle.kts` or `settings.gradle` file. Using this script won't work from
 * a regular build script.
 *
 * The centralized Gradle build infrastructure installs the following in a Gradle build:
 * - A common suite of settings-time plugins (Gradle Enterprise, Foojay, Common User Data, etc.)
 * - A common suite of build convention plugins (within the `elide.build.*` namespace, see repo for more)
 * - A suite of tailored repository settings, via Pkgst (https://pkg.st)
 *
 */
