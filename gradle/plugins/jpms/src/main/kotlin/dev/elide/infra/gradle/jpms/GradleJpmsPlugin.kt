package dev.elide.infra.gradle.jpms

import dev.elide.infra.gradle.GradleBaselinePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.getValue

/**
 * # Gradle JPMS Plugin
 *
 * This plugin is used to enhance integration between Gradle and JPMS, the Java Platform Module System. JPMS moves most
 * dependency calculations to the "module path," instead of the traditional classpath. Gradle is talented at moving
 * dependencies which look like modules to the modulepath, but it doesn't do much more than that.
 *
 * This plug-in helps by providing some basic wiring:
 *
 * - A dedicated configuration for the module-path, called `modulepath`
 * - Translation of convention config to JPMS settings
 * - Application of baselines
 *
 * Usually, you'll want to use a higher-order plugin, like `dev.elide.jlink` or `dev.elide.jmod`, rather than applying
 * this one directly.
 *
 * &nbsp;
 *
 * ## Modulepath Configuration
 *
 * The `modulepath` configuration injected by this module will itself inject into the `compileClasspath`. This results
 * in modules being added to both the `compileClasspath` and `runtimeClasspath`. If this isn't desired, you should add
 * your modules as regular dependencies to the applicable configuration.
 *
 * @see GradleBaselinePlugin for baselines applied through this plugin
 */
public abstract class GradleJpmsPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.pluginManager.apply(GradleBaselinePlugin::class.java)

    // create a dedicated module-path configuration. it should inject into the compile classpath, which should also
    // include members on the runtime classpath.
    val modulepath: Configuration by target.configurations.creating
    target.configurations.named("compileClasspath").configure {
      extendsFrom(modulepath)
    }
  }
}
