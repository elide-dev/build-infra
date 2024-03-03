package dev.elide.infra.gradle

import dev.elide.infra.gradle.api.ElideCatalogSettings
import dev.elide.infra.gradle.api.ElideCatalogSettings.ElideCatalogSettingsDsl
import dev.elide.infra.gradle.catalogs.VersionCatalogMergeTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

/**
 * # Infra Catalogs
 */
public class InfraCatalogConvention : Plugin<Project> {
  override fun apply(target: Project) {
    target.pluginManager.apply(BuildConstants.KnownPlugins.BASE)
    target.pluginManager.apply(RootBuildConvention::class.java)
    target.pluginManager.apply(ElideLibraryConvention::class.java)
    target.extensions.create(BuildConstants.Extensions.CATALOG, ElideCatalogSettingsDsl::class.java)

    target.extensions.configure<ElideCatalogSettings> {
      val merged = "mergedCatalog/catalog.versions.toml"
      val srcs = catalogs.files

      val mergeCatalogs = target.tasks.register("mergeCatalogs", VersionCatalogMergeTask::class.java) {
        group = "build"
        description = "Build merged Version Catalog target"
        catalogs = this@configure.catalogs
        destinationFile.set(target.layout.projectDirectory.file(merged))
        overrides.addAll(listOf("kotlin"))
      }

      target.tasks.named("generateCatalogAsToml").configure {
        dependsOn(mergeCatalogs)
        inputs.files(srcs)
      }

      // add a stubbed test task
      val test by target.tasks.register("test") { /* Nothing yet. */ }
    }
  }
}
