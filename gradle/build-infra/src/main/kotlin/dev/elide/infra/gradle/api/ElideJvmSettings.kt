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
   * ## JVM Target
   *
   * Sets the target JVM bytecode level
   */
  public val target: Property<JvmTarget>

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

  // JVM settings DSl.
  public class ElideJvmSettingsDsl(factory: ObjectFactory) :
    ElideJvmSettings,
    ProjectConvention<ElideJvmSettings>(ElideJvmSettings::class, factory) {
    override val target: Property<JvmTarget> = factory
      .property(JvmTarget::class.java)
      .convention(JvmTarget.DEFAULT)

    override val toolchain: Property<JavaVersion> = factory
      .property(JavaVersion::class.java)
      .convention(JavaVersion.current())

    override val vendor: Property<JvmVendorSpec> = factory
      .property(JvmVendorSpec::class.java)
      .convention(JvmVendorSpec.GRAAL_VM)

    override val implementation: Property<JvmImplementation> = factory
      .property(JvmImplementation::class.java)
      .convention(JvmImplementation.VENDOR_SPECIFIC)

    override val preview: Property<Boolean> = factory
      .property(Boolean::class.java)
      .convention(false)
  }
}
