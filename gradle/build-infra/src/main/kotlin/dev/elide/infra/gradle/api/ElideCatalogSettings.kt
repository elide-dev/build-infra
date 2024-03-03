package dev.elide.infra.gradle.api

import dev.elide.infra.gradle.base.ProjectConvention
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import javax.inject.Inject

/**
 * # Elide Catalog Settings
 */
public interface ElideCatalogSettings : Convention<ElideCatalogSettings, Project> {
  /**
   * ## Catalogs
   *
   * Version catalogs to use as inputs to the merge operation
   */
  @get:InputFiles public val catalogs: ConfigurableFileCollection

  /**
   * ## Destination File
   *
   * Target file, where the merged catalog should be written to
   */
  @get:OutputFile public val destinationFile: RegularFileProperty

  /**
   * ## Overrides
   *
   * Strings to allowlist for overrides; when collisions are encountered, they are checked for any of these substrings.
   * Matches allow the override, failures halt the build.
   */
  @get:Input public val overrides: ListProperty<String>

  // Catalog settings DSL.
  public abstract class ElideCatalogSettingsDsl @Inject constructor (factory: ObjectFactory) :
    ElideCatalogSettings,
    ProjectConvention<ElideCatalogSettings>(ElideCatalogSettings::class, factory) {
    // Input catalogs.
    @get:InputFiles override val catalogs: ConfigurableFileCollection = factory.fileCollection()

    // Destination output file.
    @get:OutputFile override val destinationFile: RegularFileProperty = factory.fileProperty()

    // Override allowances.
    @get:OutputFile override val overrides: ListProperty<String> = factory.listProperty(String::class.java)
  }
}
