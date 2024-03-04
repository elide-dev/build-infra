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
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.jvm.toolchain.JvmImplementation
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/**
 * # Elide Extension: JVM
 *
 * Configures JDK and JVM settings for Java or Kotlin JVM projects that are part of the build-infra project.
 */
public interface ElideJvmSettings : Convention<ElideJvmSettings, Project> {
  /**
   * ## JVM: JPMS Settings
   *
   * Governs features which enable Java Platform Module System (JPMS) support for Gradle JVM projects
   */
  public interface ElideJpmsSettings : Convention<ElideJpmsSettings, Project> {
    /**
     * ## JPMS Enabled
     *
     * Enable or disable JPMS integration with Gradle
     */
    public val enabled: Property<Boolean>

    /**
     * ## JMod Enabled
     *
     * Enable or disable `jmod` artifact builds
     */
    public val jmod: Property<Boolean>

    /**
     * ## JLink Enabled
     *
     * Enable or disable `jlink` builds; defaults to `true` for application targets
     */
    public val jlink: Property<Boolean>

    /**
     * ## JPMS Module
     *
     * Name of the JPMS module to build with this project; best-effort is made to detect this if not set
     */
    public val module: Property<String>
  }

  /**
   * ## JVM: JMod Settings
   *
   * Allows customization of `jmod` artifact builds
   */
  public interface ElideJModSettings : Convention<ElideJModSettings, Project> {

  }

  /**
   * ## JVM: JLink Settings
   *
   * Allows customization of `jlink` artifact builds
   */
  public interface ElideJLinkSettings : Convention<ElideJLinkSettings, Project> {

  }

  /**
   * ## JVM Target
   *
   * Sets the target JVM bytecode level
   */
  public val target: Property<JvmTarget>

  /**
   * ## JVM Minimum
   *
   * Sets the minimum JVM bytecode level
   */
  public val minimum: Property<JvmTarget>

  /**
   * ## Toolchain Target
   *
   * Sets the toolchain JDK level
   */
  public val toolchain: Property<JavaVersion>

  /**
   * ## Toolchain Vendor
   *
   * Sets the toolchain JDK vendor
   */
  public val vendor: Property<JvmVendorSpec>

  /**
   * ## Toolchain Vendor
   *
   * Sets the toolchain JDK vendor
   */
  public val implementation: Property<JvmImplementation>

  /**
   * ## Preview Features
   *
   * Enables preview features at runtime and compile time
   */
  public val preview: Property<Boolean>

  /**
   * ## Multi-Release JAR
   *
   * Enables functionality for building multi-target JAR artifacts; defaults to `true`
   */
  public val mrjar: Property<Boolean>

  /**
   * ## JPMS Settings
   *
   * Allows adjustment of JPMS-related settings (for modular applications and libraries)
   */
  public val jpms: ElideJpmsSettings

  /**
   * ## JMod Settings
   *
   * Allows adjustment of `jmod` artifact builds, as applicable
   */
  public val jmod: ElideJModSettings

  /**
   * ## JLink Settings
   *
   * Allows adjustment of `jlink` artifact builds, as applicable
   */
  public val jlink: ElideJLinkSettings

  // JVM JPMS settings DSL.
  public class ElideJpmsSettingsDsl(factory: ObjectFactory) :
    ElideJpmsSettings,
    ProjectConvention<ElideJpmsSettings>(ElideJpmsSettings::class, factory) {
    // Whether JPMS is enabled.
    override val enabled: Property<Boolean> = factory
      .property(Boolean::class.java)
      .convention(true)

    // Whether JMod builds are enabled.
    override val jmod: Property<Boolean> = factory
      .property(Boolean::class.java)
      .convention(true)

    // Whether JLink is enabled; defaults to `true` for application targets.
    override val jlink: Property<Boolean> = factory
      .property(Boolean::class.java)

    // Module name under construction.
    override val module: Property<String> = factory
      .property(String::class.java)
  }

  // JVM JMod settings DSL.
  public class ElideJModSettingsDsl(factory: ObjectFactory) :
    ElideJModSettings,
    ProjectConvention<ElideJModSettings>(ElideJModSettings::class, factory) {

  }

  // JVM JLink settings DSL.
  public class ElideJLinkSettingsDsl(factory: ObjectFactory) :
    ElideJLinkSettings,
    ProjectConvention<ElideJLinkSettings>(ElideJLinkSettings::class, factory) {
    //
  }

  // JVM settings DSL.
  public class ElideJvmSettingsDsl(factory: ObjectFactory) :
    ElideJvmSettings,
    ProjectConvention<ElideJvmSettings>(ElideJvmSettings::class, factory) {
    // Internal JPMS settings.
    private val jpmsSettings = ElideJpmsSettingsDsl(factory)

    // Internal JMod settings.
    private val jmodSettings = ElideJModSettingsDsl(factory)

    // Internal JLink settings.
    private val jlinkSettings = ElideJLinkSettingsDsl(factory)

    // Main target for JVM bytecode.
    override val target: Property<JvmTarget> = factory
      .property(JvmTarget::class.java)

    // Minimum target for JVM bytecode.
    override val minimum: Property<JvmTarget> = factory
      .property(JvmTarget::class.java)

    // Toolchain language version.
    override val toolchain: Property<JavaVersion> = factory
      .property(JavaVersion::class.java)
      .convention(JavaVersion.current())

    // Vendor for toolchain JDK.
    override val vendor: Property<JvmVendorSpec> = factory
      .property(JvmVendorSpec::class.java)
      .convention(JvmVendorSpec.GRAAL_VM)

    // Implementation for toolchain JDK.
    override val implementation: Property<JvmImplementation> = factory
      .property(JvmImplementation::class.java)
      .convention(JvmImplementation.VENDOR_SPECIFIC)

    // Whether to enable Java preview features.
    override val preview: Property<Boolean> = factory
      .property(Boolean::class.java)
      .convention(false)

    // Whether to enable multi-release JARs; defaults to `true` and kicks in when needed.
    override val mrjar: Property<Boolean> = factory
      .property(Boolean::class.java)
      .convention(true)

    // Settings which govern modular Java features.
    override val jpms: ElideJpmsSettings = jpmsSettings

    // Settings which govern JMod artifacts.
    override val jmod: ElideJModSettings = jmodSettings

    // Settings which govern JLink artifacts.
    override val jlink: ElideJLinkSettings = jlinkSettings
  }
}
