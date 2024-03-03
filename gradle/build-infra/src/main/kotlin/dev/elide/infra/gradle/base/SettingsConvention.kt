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

package dev.elide.infra.gradle.base

import dev.elide.infra.gradle.api.Convention
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.the
import kotlin.reflect.KClass

// Abstract convention for settings types.
public abstract class SettingsConvention<C> protected constructor (
  private val type: KClass<C>,
  factory: ObjectFactory,
) : AbstractConvention<C, Settings>(factory) where C: Convention<C, Settings> {
  override fun type(): KClass<C> = type
}
