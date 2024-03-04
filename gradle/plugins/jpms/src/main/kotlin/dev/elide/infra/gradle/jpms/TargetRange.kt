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

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.SortedSet
import java.util.stream.Stream

/**
 * ## Target Range
 *
 * Describes a range of JVM target support from a [minimum] to a [maximum]
 */
public interface TargetRange {
  /**
   * ### Minimum
   *
   * Minimum supported JVM target within this range
   */
  public val minimum: JvmTarget

  /**
   * ### Maximum
   *
   * Maximum supported JVM target within this range
   */
  public val maximum: JvmTarget

  /**
   * ### All Targets
   *
   * Return a stream of all targets included within the range, regardless of applicability
   */
  public val all: Stream<JvmTarget>

  /**
   * ### Applicable Targets
   *
   * Return a stream of all applicable targets within the range; defined as all targets within the range which are not
   * "ignorable" (mostly major or LTS releases)
   *
   * @param ignore Targets to ignore
   * @return Stream of applicable targets
   */
  public fun applicable(ignore: SortedSet<JvmTarget>): Stream<JvmTarget>

  /**
   * ### Contains
   *
   * Indicates whether the range contains the [target]
   *
   * @param target JVM target to check
   * @return Whether the range supports this target
   */
  public operator fun contains(target: JvmTarget): Boolean
}
