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
import kotlin.reflect.KClass

/**
 * # Convention
 *
 * Top-level concept of a convention, which is configurable via direct invocation.
 */
public interface Convention<Context, Target>
  where Context: Convention<Context, Target>,
        Target: ExtensionAware,
        Target: PluginAware {
  /**
   * ## API Type
   *
   * Return the class which describes this convention's configuration API
   *
   * @return API layout type
   */
  public fun type(): KClass<Context>

  /**
   * ## Configure
   *
   * Invoke this extension, configuring its context.
   *
   * @param op Operation to run within the context extension instance
   */
  public operator fun Target.invoke(op: Context.() -> Unit)
}
