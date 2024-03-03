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

package dev.elide.infra.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * # Convention: Root Build
 *
 * Configures dependencies and settings which are unique to the root build; provided at `elide.root`.
 */
public class RootBuildConvention : Plugin<Project> {
  override fun apply(target: Project) {
    target.pluginManager.apply(BaselinePlugin::class.java)
  }
}
