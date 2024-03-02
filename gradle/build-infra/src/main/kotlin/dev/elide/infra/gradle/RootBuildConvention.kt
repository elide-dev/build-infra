package dev.elide.infra.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * # Convention: Root Build
 *
 * Configures dependencies and settings which are unique to the root build; provided at `elide.root`.
 */
public class RootBuildConvention : Plugin<Project> {
  override fun apply(target: Project) {
    target.pluginManager.apply(BaselinePlugin::class.java)
  }
}
