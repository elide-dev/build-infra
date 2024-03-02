package dev.elide.infra.gradle.jvm

import dev.elide.infra.gradle.BuildConstants
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * # JVM: Baseline Conventions
 */
public abstract class BaselineJvmConventions : Plugin<Project> {
  override fun apply(target: Project) {
    // `java` plugin is always included
    target.pluginManager.apply(BuildConstants.KnownPlugins.JAVA)

    // next up are toolchains
    target.pluginManager.apply(JvmToolchainConvention::class.java)

    // finally, target and task conventions
    target.pluginManager.apply(JvmTargetConventions::class.java)
  }
}
