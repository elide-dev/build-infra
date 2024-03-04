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

package dev.elide.infra.gradle.jlink

import dev.elide.infra.gradle.api.javaHomeFile
import dev.elide.infra.gradle.api.projectRelative
import dev.elide.infra.gradle.jmod.GradleJModPlugin
import dev.elide.infra.gradle.jpms.GradleJpmsPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.CommandLineArgumentProvider

/**
 * # Gradle JLink Plugin
 */
public class GradleJLinkPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    // always apply the jpms plugin
    if (!target.plugins.hasPlugin(GradleJpmsPlugin::class.java))
      target.pluginManager.apply(GradleJpmsPlugin::class.java)

    // always apply the jmod plugin
    if (!target.plugins.hasPlugin(GradleJModPlugin::class.java))
      target.pluginManager.apply(GradleJModPlugin::class.java)

    // build expected input and output paths
    val jlinkOut = target.layout.buildDirectory.dir("jlink").get()

    // resolve dependency tasks
    val javac = target.tasks.findByName("compileJava")
    val jmod = target.tasks.findByName("jmod")
    val jar = target.tasks.findByName("jar")
      ?: target.tasks.findByName("jvmJar")
    val kotlinc = target.tasks.findByName("compileKotlin")
      ?: target.tasks.findByName("compileKotlinJvm")

    val dependencies = listOfNotNull(
      javac,
      kotlinc,
      jar,
      jmod,
    )

    target.tasks.register("jlink", JLinkTask::class.java) {
      dependsOn(dependencies)
      destinationDirectory.set(jlinkOut.asFile)
    }
  }
}

/**
 * # Tasks: JLink
 */
@Suppress("LeakingThis")
public abstract class JLinkTask : Exec() {
  /**
   * JLink arguments to include unconditionally
   */
  @get:Input public abstract val args: ListProperty<String>

  /**
   * Configuration to use for the module path
   */
  @get:Input public abstract val modulepath: Property<Configuration>

  /**
   * JLink arguments to include unconditionally
   */
  @get:OutputDirectory
  public abstract val destinationDirectory: DirectoryProperty

  init {
    args.convention(listOf(
      "--output=${project.projectRelative(destinationDirectory.get().asFile.toPath())}",
    ))
  }

  /**
   * Argument providers to consider
   */
  @get:Input public abstract val argumentProviders: ListProperty<CommandLineArgumentProvider>

  @TaskAction public fun jlink() {
    executable = project.javaHomeFile("bin", "jlink")
    outputs.dir(project.layout.buildDirectory.dir("jlink"))

    // JARs to exclude from the class path for the native-image build.
    argumentProviders.add(CommandLineArgumentProvider {
      listOf(
        "--module-path",
        (modulepath.get()).asPath,
      )
    })
  }
}
