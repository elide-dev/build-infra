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

package dev.elide.infra.gradle.testing

import java.util.SortedSet

/** Functions to which the Power Assert plug-in should be applied, unless otherwise specified. */
public val powerAssertDefaultFunctions: SortedSet<String> = sortedSetOf(
  "kotlin.assert",
  "kotlin.test.assertContains",
  "kotlin.test.assertContentEquals",
  "kotlin.test.assertEquals",
  "kotlin.test.assertFails",
  "kotlin.test.assertFailsWith",
  "kotlin.test.assertFalse",
  "kotlin.test.assertIs",
  "kotlin.test.assertIsNot",
  "kotlin.test.assertNotContains",
  "kotlin.test.assertNotEquals",
  "kotlin.test.assertNotNull",
  "kotlin.test.assertNotSame",
  "kotlin.test.assertNull",
  "kotlin.test.assertSame",
  "kotlin.test.assertTrue",
  "kotlin.test.expect",
  "kotlin.test.fail",
  "org.junit.jupiter.api.assertDoesNotThrow",
  "org.junit.jupiter.api.assertThrows",
)
