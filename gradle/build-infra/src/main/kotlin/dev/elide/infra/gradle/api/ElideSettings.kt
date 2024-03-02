package dev.elide.infra.gradle.api

import dev.elide.infra.gradle.base.SettingsConvention
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

/**
 * # Elide Extension: Settings
 *
 * Example of use:
 * ```kotlin
 * // in a settings.gradle.kts
 * plugins {
 *   id("elide.settings")
 * }
 *
 * infra {
 *   buildCaching {
 *    local.enabled = false
 *   }
 * }
 * ```
 */
public sealed interface ElideSettings : ElideExtension<ElideSettings, Settings> {
  /**
   * Local build cache settings
   */
  public interface LocalBuildCacheSettings : Convention<LocalBuildCacheSettings, Settings> {
    /**
     * Whether the local build cache is enabled or not
     */
    public val enabled: Property<Boolean>
  }

  /**
   * ### Build Caching Settings
   *
   * API for adjusting settings-time configuration related to build caching.
   */
  public interface BuildCachingSettings : Convention<BuildCachingSettings, Settings> {
    /**
     * ### Local
     *
     * Adjust local build cache settings.
     */
    public var local: LocalBuildCacheSettings

    /**
     * ### Buildless
     *
     * Whether to enable and configure Buildless
     */
    public val buildless: Property<Boolean>
  }

  /**
   * ### Repository Settings
   *
   * API for adjusting settings-time repository configuration.
   */
  public interface RepositoryConfiguration : Convention<RepositoryConfiguration, Settings> {
    /**
     * Whether to enable Pkgst repositories
     */
    public val pkgst: Property<Boolean>
  }

  /**
   * ## Build Caching
   *
   * Adjust settings-time build caching configuration
   */
  public val buildCaching: BuildCachingSettings

  /**
   * ## Repositories
   *
   * Adjust settings-time repository settings
   */
  public val repositories: RepositoryConfiguration

  // Local build caching settings receiver.
  public class ElideLocalBuildCachingSettingsDsl internal constructor (
    factory: ObjectFactory,
  ) : LocalBuildCacheSettings,
      SettingsConvention<LocalBuildCacheSettings>(LocalBuildCacheSettings::class, factory) {
    override var enabled: Property<Boolean> = factory
      .property(Boolean::class.java)
      .convention(true)
  }

  // Build caching settings receiver.
  public class ElideBuildCachingSettingsDsl internal constructor (factory: ObjectFactory)
    : BuildCachingSettings,
      SettingsConvention<BuildCachingSettings>(BuildCachingSettings::class, factory) {
    override var local: LocalBuildCacheSettings = ElideLocalBuildCachingSettingsDsl(factory)
    override var buildless: Property<Boolean> = factory
      .property(Boolean::class.java)
      .convention(true)
  }

  // Repository settings receiver.
  public class ElideRepositorySettingsDsl internal constructor (factory: ObjectFactory)
    : RepositoryConfiguration,
      SettingsConvention<RepositoryConfiguration>(RepositoryConfiguration::class, factory) {
    override var pkgst: Property<Boolean> = factory
      .property(Boolean::class.java)
      .convention(true)
  }

  // Top-level settings receiver.
  public abstract class ElideSettingsDsl(
    factory: ObjectFactory
  ) : ElideSettings,
      SettingsConvention<ElideSettings>(ElideSettings::class, factory) {
    private val caching = ElideBuildCachingSettingsDsl(factory)
    private val repos = ElideRepositorySettingsDsl(factory)

    override val repositories: RepositoryConfiguration get() = repos
    override val buildCaching: BuildCachingSettings get() = caching
  }
}
