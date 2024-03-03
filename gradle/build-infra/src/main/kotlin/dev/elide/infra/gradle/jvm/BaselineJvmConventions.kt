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

package dev.elide.infra.gradle.jvm

import dev.elide.infra.gradle.BuildConstants
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * # JVM: Baseline Conventions
 */
public abstract class BaselineJvmConventions : Plugin<Project> {
  override fun apply(target: Project) {
    // `java` plugin is always included
    target.pluginManager.apply(BuildConstants.KnownPlugins.JAVA)

    // next up are toolchains
    target.pluginManager.apply(JvmToolchainConvention::class.java)

    // finally, target and task conventions
    target.pluginManager.apply(JvmTargetConventions::class.java)
  }
}
