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

package dev.elide.infra.gradle.jpms

import dev.elide.infra.gradle.GradleBaselinePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.provider.Property
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

// Valid module versions are set as `N.N` where `N` is a positive integer.
private val moduleVersionRegex = Regex("^(\\d+\\.\\d+)$")

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
  // Implements the Gradle plugin configuration extension.
  internal abstract class ConfiguredJpmsExtension @Inject constructor (factory: ObjectFactory) : GradleJpmsExtension {
    // Default to `true` for enablement.
    override val enabled: Property<Boolean> = factory.property(Boolean::class).convention(true)

    // Align versions by default.
    override val alignModuleVersion: Property<Boolean> = factory.property(Boolean::class).convention(true)

    // Align main class by default.
    override val alignMainClass: Property<Boolean> = factory.property(Boolean::class).convention(true)
  }

  override fun apply(target: Project) {
    target.pluginManager.apply(GradleBaselinePlugin::class.java)

    // create a dedicated module-path configuration. it should inject into the compiler classpath, which should also
    // include members on the runtime classpath.
    val modulepath: Configuration by target.configurations.creating {
      isCanBeResolved = true
    }

    // factory the extension which provides configurability of jpms
    target.extensions.create(
      GradleJpmsExtension::class.java,
      GradleJpmsExtension.NAME,
      ConfiguredJpmsExtension::class.java,
    )

    // mount the special `modulepath` configuration into the compiler classpath
    target.configurations.named("compileClasspath").configure {
      val ext = target.extensions.getByType(GradleJpmsExtension::class.java)
      if (ext.enabled.get()) {
        extendsFrom(modulepath)
      }
    }

    // configure compile tasks for modularity awareness
    target.tasks.withType(JavaCompile::class.java).configureEach {
      val ext = target.extensions.getByType(GradleJpmsExtension::class.java)
      if (!ext.enabled.get()) { return@configureEach }

      // activate modularity conventions
      modularity.inferModulePath.convention(true)

      // assign aligned version
      if (ext.alignModuleVersion.get()) {
        (target.version as? String)?.let {
          when (it) {
            // do not set the module version for unspecified project versions
            "unspecified" -> target.logger.debug("Project version is unspecified; skipping module version")
            else -> if (it.isNotEmpty() && it.isNotBlank() && it.matches(moduleVersionRegex)) {
              options.javaModuleVersion.convention(it)
            } else {
              target.logger.warn("Project version '$it' is not a valid JPMS module version; discarding")
            }
          }
        }
      }

      // assign the main class within compiled module-info, but only for the main source set
      if (name == "compileJava" && ext.alignMainClass.get()) {
        target.extensions.findByType(JavaApplication::class)?.apply {
          if (mainClass.isPresent) {
            options.javaModuleMainClass.convention(mainClass.get())
          }
        }
      }
    }
  }
}
