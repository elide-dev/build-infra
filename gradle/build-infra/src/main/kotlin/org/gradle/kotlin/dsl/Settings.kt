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

@file:Suppress("UnstableApiUsage")

package org.gradle.kotlin.dsl

import dev.elide.infra.gradle.api.ElideSettings
import org.gradle.api.initialization.Settings

/**
 * ## Settings: Infra Setup
 *
 * Setup conventions at settings time for a build infrastructure module.
 *
 * @param op Function to execute to build settings-time conventions
 */
public fun Settings.infra(op: ElideSettings.() -> Unit) {
  pluginManager.withPlugin("infra.settings") {
    the<ElideSettings.ElideSettingsDsl>().apply(op)
  }
}
