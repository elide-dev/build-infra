package dev.elide.infra.gradle

import dev.elide.infra.gradle.kotlin.KotlinJvmConventions
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * # Plugins: Kotlin Infrastructure (JVM)
 *
 * Extends the baseline Kotlin infrastructure plug-in ([ElideKotlin]) with JVM-specific functionality; applies JVM and
 * Java baselines.
 */
public class ElideKotlinJvm : Plugin<Project> {
  override fun apply(target: Project) {
    target.pluginManager.apply(ElideJava::class.java)
    target.pluginManager.apply(ElideKotlin::class.java)
    target.pluginManager.withPlugin(BuildConstants.Conventions.KOTLIN) {
      target.pluginManager.apply(KotlinJvmConventions::class.java)
    }
  }
}
