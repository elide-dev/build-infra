package dev.elide.infra.gradle.mrjar

import dev.elide.infra.gradle.jpms.GradleJpmsPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * # Gradle MRJAR Plugin
 */
public abstract class GradleMultiReleaseJarPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    // always apply the baseline jpms plugin
    if (!target.plugins.hasPlugin(GradleJpmsPlugin::class.java))
      target.pluginManager.apply(GradleJpmsPlugin::class.java)
  }
}
