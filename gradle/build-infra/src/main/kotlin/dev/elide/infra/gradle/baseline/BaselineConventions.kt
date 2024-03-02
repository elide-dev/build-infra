package dev.elide.infra.gradle.baseline

import dev.elide.infra.gradle.BuildConstants
import dev.elide.infra.gradle.api.ElideBuild
import org.gradle.api.Plugin
import org.gradle.api.Project

private fun allProjectsPlugins(): List<String> = listOf(
  // None yet.
)

/**
 * # Conventions: Baseline
 */
public class BaselineConventions : Plugin<Project> {
  override fun apply(target: Project) {
    target.group = "dev.elide.infra"

    allProjectsPlugins().forEach { pluginId ->
      target.pluginManager.apply(pluginId)
    }

    // create the baseline settings-time extension
    if (target.extensions.findByName(BuildConstants.Extensions.META) == null)
      target.extensions.create(BuildConstants.Extensions.META, ElideBuild.ElideBuildDsl::class.java)
  }
}
