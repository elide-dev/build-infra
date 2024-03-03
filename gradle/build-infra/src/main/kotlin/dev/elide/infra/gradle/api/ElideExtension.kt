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

package dev.elide.infra.gradle.api

import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.PluginAware

/**
 * # Elide Extension: Conventions
 */
public sealed interface ElideExtension<E, T> : Convention<E, T>
  where E: ElideExtension<E, T>,
        T: ExtensionAware,
        T: PluginAware
