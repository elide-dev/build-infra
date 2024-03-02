package dev.elide.infra.gradle.api

import dev.elide.infra.gradle.base.ProjectConvention
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory

/**
 * # Elide Extension: Build
 */
public sealed interface ElideBuild : ElideExtension<ElideBuild, Project> {
  /**
   * ## JVM Settings
   *
   * Provides control over JVM targeting, toolchains, and other related settings.
   */
  public val jvm: ElideJvmSettings

  /**
   * ## Kotlin Settings
   *
   * Provides control over Kotlin compiler tasks and related settings.
   */
  public val kotlin: ElideKotlinSettings

  // Build (project-level) configuration DSL.
  public abstract class ElideBuildDsl(factory: ObjectFactory) :
    ElideBuild,
    ProjectConvention<ElideBuild>(ElideBuild::class, factory) {
    // JVM conventions.
    override val jvm: ElideJvmSettings = ElideJvmSettings.ElideJvmSettingsDsl(factory)

    // Kotlin feature conventions.
    override val kotlin: ElideKotlinSettings = ElideKotlinSettings.ElideKotlinSettingsDsl(factory)
  }
}
