package dev.elide.infra.gradle.checks

import dev.elide.infra.gradle.BuildConstants
import dev.elide.infra.gradle.api.ElideBuild
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektPlugin
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import io.gitlab.arturbosch.detekt.report.ReportMergeTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

// Root-relative path to the Detekt configuration file.
private const val detektConfig = "config/detekt/detekt.yml"

// Root-relative path to the main Detekt report file, in XML format.
private const val mergedReportXml = "reports/detekt/detekt.xml"

// Root-relative path to the main Detekt report file, in SARIF format.
private const val mergedReportSarif = "reports/detekt/detekt.sarif"

// Reporting kill-switch.
private const val enableReportingByDefault = false

// Merged reporting kill-switch.
private const val mergedReportingByDefault = false

/**
 * # Checks: Detekt Convention
 */
public class DetektConvention : Plugin<Project> {
  override fun apply(target: Project) {
    target.pluginManager.apply(DetektPlugin::class.java)

    target.afterEvaluate {
      extensions.getByType(ElideBuild.ElideBuildDsl::class.java).let { conventions ->
        target.configureDetektExtension(conventions)
      }
    }
  }
}

// Configure uniform Detekt settings for a project.
private fun Project.configureDetektExtension(conventions: ElideBuild) = extensions.configure<DetektExtension> {
  parallel = true
  buildUponDefaultConfig = true
  enableCompilerPlugin.set(true)
  basePath = rootProject.projectDir.absolutePath
  ignoreFailures = conventions.checks.ignoreFailures.get()
  if (rootProject.file(detektConfig).exists()) {
    config.from(rootProject.files(detektConfig))
  }

  val detektMergeSarif = tasks.register(BuildConstants.TaskName.DETEKT_MERGE_SARIF, ReportMergeTask::class.java) {
    isEnabled = mergedReportingByDefault
    output.set(rootProject.layout.buildDirectory.file(mergedReportSarif))
  }
  val detektMergeXml = tasks.register(BuildConstants.TaskName.DETEKT_MERGE_XML, ReportMergeTask::class.java) {
    isEnabled = mergedReportingByDefault
    output.set(rootProject.layout.buildDirectory.file(mergedReportXml))
  }

  plugins.withType(DetektPlugin::class) {
    tasks.withType(Detekt::class) detekt@{
      if (enableReportingByDefault) {
        if (mergedReportingByDefault) finalizedBy(detektMergeSarif, detektMergeXml)
        reports.sarif.required.set(true)
        reports.xml.required.set(true)
        reports.sarif.outputLocation.set(layout.buildDirectory.file(mergedReportSarif))
        reports.xml.outputLocation.set(layout.buildDirectory.file(mergedReportXml))
      }

      detektMergeSarif.configure {
        input.from(this@detekt.sarifReportFile)
      }
      detektMergeXml.configure {
        input.from(this@detekt.xmlReportFile)
      }
    }
  }
}
