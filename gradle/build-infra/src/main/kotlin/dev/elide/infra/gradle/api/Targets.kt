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

import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.SortedSet
import java.util.stream.Stream

// Ordinal identity of JVM targets.
public val jvmOrdinalIdentity: Map<Int, JvmTarget> = listOf(
  JvmTarget.JVM_1_8,
  JvmTarget.JVM_9,
  JvmTarget.JVM_10,
  JvmTarget.JVM_11,
  JvmTarget.JVM_12,
  JvmTarget.JVM_13,
  JvmTarget.JVM_14,
  JvmTarget.JVM_15,
  JvmTarget.JVM_16,
  JvmTarget.JVM_17,
  JvmTarget.JVM_18,
  JvmTarget.JVM_19,
  JvmTarget.JVM_20,
  JvmTarget.JVM_21,
).associateBy {
  it.ordinal
}

// LTS releases which are included when building a range.
public val jvmLtsReleases: SortedSet<JvmTarget> = sortedSetOf(
  JvmTarget.JVM_1_8,
  JvmTarget.JVM_11,
  JvmTarget.JVM_17,
  JvmTarget.JVM_21,
)

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

/**
 * Build a range of JVM targets.
 *
 * @receiver Base target
 * @param to Ultimate target
 * @return Target range
 */
public infix fun JvmTarget.until(to: JvmTarget): TargetRange {
  return JvmTargetRange((ordinal until (to.ordinal + 1)).map {
    requireNotNull(jvmOrdinalIdentity[it])
  }.toSortedSet())
}

// Holds a range of JVM targets, from a minimum to a maximum.
@JvmInline internal value class JvmTargetRange(private val range: SortedSet<JvmTarget>): TargetRange {
  // Minimum supported JVM target.
  override val minimum: JvmTarget get() = range.first()

  // Maximum supported JVM target.
  override val maximum: JvmTarget get() = range.last()

  // Contains check.
  override operator fun contains(target: JvmTarget): Boolean = target in range

  // Stream of all supported JVM targets within the range.
  override val all: Stream<JvmTarget> get() = range.stream()

  // Stream of supported JVM targets within the range, without ignorable targets.
  override fun applicable(ignore: SortedSet<JvmTarget>): Stream<JvmTarget> = range.stream().filter {
    it !in ignore
  }
}
