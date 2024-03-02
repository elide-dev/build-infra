package dev.elide.infra.gradle.api

import dev.elide.infra.gradle.base.ProjectConvention
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import kotlin.reflect.KClass

/**
 * # Elide Extension: Kotlin
 *
 * Configures Kotlin platform and target settings for a given build-infra project.
 */
public interface ElideKotlinSettings : Convention<ElideKotlinSettings, Project> {
  /**
   * ## Kotlin API Target
   *
   * Sets the target version for Kotlin API interop
   */
  public val api: Property<KotlinVersion>

  /**
   * ## Kotlin API Target
   *
   * Sets the target version for Kotlin source code
   */
  public val language: Property<KotlinVersion>

  /**
   * ## Kotlin Opt-ins
   *
   * Well-qualified opt-in names
   */
  public val optIns: ListProperty<String>

  /**
   * Add an opt-in using the provided class [name]
   *
   * @param name Well-qualified class name to opt-into
   */
  public fun optIn(name: String) {
    optIns.add(name)
  }

  /**
   * Add an opt-in using the provided [klass]
   *
   * @param klass Class to opt in with via Kotlin
   */
  public fun optIn(klass: KClass<*>) {
    optIns.add(requireNotNull(klass.qualifiedName))
  }

  // Kotlin settings DSL.
  public class ElideKotlinSettingsDsl(factory: ObjectFactory) :
    ElideKotlinSettings,
    ProjectConvention<ElideKotlinSettings>(ElideKotlinSettings::class, factory) {
    // API target.
    override val api: Property<KotlinVersion> = factory
      .property(KotlinVersion::class.java)
      .convention(KotlinVersion.DEFAULT)

    // Language target.
    override val language: Property<KotlinVersion> = factory
      .property(KotlinVersion::class.java)
      .convention(KotlinVersion.DEFAULT)

    // Kotlin opt-ins.
    override val optIns: ListProperty<String> = factory
      .listProperty(String::class.java)
      .convention(mutableListOf<String>())
  }
}
