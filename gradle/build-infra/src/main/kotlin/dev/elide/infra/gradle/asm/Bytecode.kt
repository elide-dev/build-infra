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

@file:Suppress("RemoveRedundantQualifierName")

package dev.elide.infra.gradle.asm

import org.gradle.internal.impldep.org.objectweb.asm.Opcodes
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.*

/**
 * # General Bytecode Utilities
 */
public object Bytecode {
  /**
   * ## JVM Target from Bytecode Level
   *
   * Identifies a [JvmTarget] level by a classfile/bytecode [version]
   *
   * @param version Bytecode version
   * @return JVM target
   */
  @JvmStatic public fun JvmTarget.Companion.fromBytecodeLevel(version: Int): JvmTarget? = when (version) {
    Opcodes.V21 -> JVM_21
    Opcodes.V17 -> JVM_17
    Opcodes.V11 -> JVM_11
    Opcodes.V1_8 -> JVM_1_8
    Opcodes.V9 -> JVM_9
    Opcodes.V10 -> JVM_10
    Opcodes.V12 -> JVM_12
    Opcodes.V13 -> JVM_13
    Opcodes.V14 -> JVM_14
    Opcodes.V15 -> JVM_15
    Opcodes.V16 -> JVM_16
    Opcodes.V18 -> JVM_18
    Opcodes.V19 -> JVM_19
    Opcodes.V20 -> JVM_20
    else -> null
  }

  /**
   * ## Target Compatibility
   *
   * Indicate whether the [other] compat target is compatible with this one.
   *
   * @param other JVM target to check
   * @return Whether this target is compatible with the provided one (greater than or equal to)
   */
  public fun JvmTarget.compatibleWith(other: JvmTarget): Boolean = this >= other
}
