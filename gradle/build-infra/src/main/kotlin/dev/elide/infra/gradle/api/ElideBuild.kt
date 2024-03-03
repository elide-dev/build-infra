/*
 * Copyright (c) 2024 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

package dev.elide.infra.gradle.api

import dev.elide.infra.gradle.base.ProjectConvention
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.LockMode
import org.gradle.api.artifacts.verification.DependencyVerificationMode
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * # Elide Extension: Build
 *
 * Defines convention interfaces which assign to a given Gradle [Project]; this interface establishes a sealed hierarchy
 * which allows type-based selection of convention types.
 */
public sealed interface ElideBuild : ElideExtension<ElideBuild, Project> {
  /**
   * # Build: Dependency Locking
   *
   * Controls dependency locking settings
   */
  public interface ElideDependencyLockingSettings : Convention<ElideDependencyLockingSettings, Project> {
    /**
     * Whether dependency locking is enabled for this project
     */
    public val enabled: Property<Boolean>

    /**
     * Dependency locking mode to apply to this project
     */
    public val mode: Property<LockMode>

    /**
     * Ignored configurations for the purposes of locking
     */
    public val ignored: ListProperty<String>
  }

  /**
   * # Build: Dependency Verification
   *
   * Controls dependency verification settings
   */
  public interface ElideDependencyVerificationSettings : Convention<ElideDependencyVerificationSettings, Project> {
    /**
     * Whether dependency verification is enabled for this project
     */
    public val enabled: Property<Boolean>

    /**
     * Dependency verification mode to apply to this project
     */
    public val mode: Property<DependencyVerificationMode>

    /**
     * Ignored configurations for the purposes of verification
     */
    public val ignored: ListProperty<String>
  }

  /**
   * # Build: Dependency Settings
   *
   * Controls dependency resolution, locking, and verification features
   */
  public interface ElideDependencySettings : Convention<ElideDependencySettings, Project> {
    /**
     * Settings which control dependency locking features
     */
    public val locking: ElideDependencyLockingSettings

    /**
     * Settings which control dependency verification features
     */
    public val verification: ElideDependencyVerificationSettings
  }

  /**
   * ## Baselines
   *
   * Whether to apply basic (baseline) Gradle settings
   */
  public val baselines: Property<Boolean>

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

  /**
   * ## Dependency Settings
   *
   * Provides control over dependency resolution, locking, and verification settings
   */
  public val dependencies: ElideDependencySettings

  // Dependency locking settings DSL.
  public class ElideDependencyLockingSettingsDsl(factory: ObjectFactory) :
    ElideDependencyLockingSettings,
    ProjectConvention<ElideDependencyLockingSettings>(ElideDependencyLockingSettings::class, factory) {
    // Whether dependency locking is enabled.
    override val enabled: Property<Boolean> = factory
      .property(Boolean::class.java)
      .convention(true)

    // Locking mode to apply.
    override val mode: Property<LockMode> = factory
      .property(LockMode::class.java)
      .convention(LockMode.LENIENT)

    // Configuration sub-strings, matches of which are ignored for the purposes of locking.
    override val ignored: ListProperty<String> = factory
      .listProperty(String::class.java)
      .convention(listOf("detached"))
  }

  // Dependency verification settings DSL.
  public class ElideDependencyVerificationSettingsDsl(factory: ObjectFactory) :
    ElideDependencyVerificationSettings,
    ProjectConvention<ElideDependencyVerificationSettings>(ElideDependencyVerificationSettings::class, factory) {
    // Whether dependency verification is enabled.
    override val enabled: Property<Boolean> = factory
      .property(Boolean::class.java)
      .convention(true)

    // Verification mode to apply.
    override val mode: Property<DependencyVerificationMode> = factory
      .property(DependencyVerificationMode::class.java)
      .convention(DependencyVerificationMode.LENIENT)

    // Configuration sub-strings, matches of which are ignored for the purposes of verification.
    override val ignored: ListProperty<String> = factory
      .listProperty(String::class.java)
  }

  // Dependency settings DSL.
  public class ElideDependencySettingsDsl(factory: ObjectFactory) :
    ElideDependencySettings,
    ProjectConvention<ElideDependencySettings>(ElideDependencySettings::class, factory) {
    // Dependency locking settings to apply.
    override val locking: ElideDependencyLockingSettings = ElideDependencyLockingSettingsDsl(factory)

    // Dependency verification settings to apply.
    override val verification: ElideDependencyVerificationSettings = ElideDependencyVerificationSettingsDsl(factory)
  }

  // Build (project-level) configuration DSL.
  public abstract class ElideBuildDsl(factory: ObjectFactory) :
    ElideBuild,
    ProjectConvention<ElideBuild>(ElideBuild::class, factory) {
    // Whether to install baseline settings.
    override val baselines: Property<Boolean> = factory
      .property(Boolean::class.java)
      .convention(true)

    // JVM conventions.
    override val jvm: ElideJvmSettings = ElideJvmSettings.ElideJvmSettingsDsl(factory)

    // Kotlin feature conventions.
    override val kotlin: ElideKotlinSettings = ElideKotlinSettings.ElideKotlinSettingsDsl(factory)

    // Dependency feature conventions.
    override val dependencies: ElideDependencySettings = ElideDependencySettingsDsl(factory)
  }
}
