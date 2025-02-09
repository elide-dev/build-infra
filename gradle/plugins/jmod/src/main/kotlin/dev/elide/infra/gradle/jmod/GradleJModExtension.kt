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

package dev.elide.infra.gradle.jmod

import org.gradle.api.provider.Property

/**
 * # Gradle JMod Extension
 *
 * Defines the DSL extension for Gradle for configuring the JMod plugin. This plugin builds JMod files from JPMS-enabled
 * Java code. The extension is created by the [GradleJModPlugin] at application time, and configured by the user using
 * the `jmod` block in the `build.gradle.kts` file.
 */
public interface GradleJModExtension {
  public companion object {
    /** DSL name of the extension. */
    public const val NAME: String = "jmod"
  }

  /** Whether to enable the jmod plugin; if `false`, the plugin is rendered inert. */
  public val enabled: Property<Boolean>
}
