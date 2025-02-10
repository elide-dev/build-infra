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

import org.gradle.api.provider.Property

/**
 * # Gradle JPMS Extension
 *
 * Defines the configuration DSL for the Gradle JPMS plugin; this includes settings that govern the attributes affixed
 * to the final compiled module, and how modularity is applied, tested, and enforced.
 *
 * For the Gradle JPMS Extension to be visible within a user's build, the [GradleJpmsPlugin] must be applied within the
 * project's `plugins { }` block.
 */
public interface GradleJpmsExtension {
  public companion object {
    /** Name where the extension is mounted. */
    public const val NAME: String = "jpms"
  }

  /** Whether to enable the JPMS extension. When disabled, the extension and plug-in are rendered inert. */
  public val enabled: Property<Boolean>

  /** Align the module version with the project. Requires a version that can be used in a module; defaults to `true`. */
  public val alignModuleVersion: Property<Boolean>

  /** Align the module main-class with the project, if applicable. Defaults to `true`. */
  public val alignMainClass: Property<Boolean>
}
