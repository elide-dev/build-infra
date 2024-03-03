package dev.elide.infra.gradle

import dev.elide.infra.gradle.baseline.BaselineConventions
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * # Elide: Gradle Plugin Convention
 *
 * Applies conventions for building and shipping a published Gradle plugin, from the `build-infra` project
 */
public class ElideGradlePlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.pluginManager.apply(BaselineConventions::class.java)
    target.pluginManager.apply(BuildConstants.KnownPlugins.JAVA_GRADLE_PLUGIN)
    target.pluginManager.apply(BuildConstants.KnownPlugins.GRADLE_PLUGIN_PUBLISH)
  }
}
