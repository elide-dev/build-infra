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

package dev.elide.infra.gradle.asm

import dev.elide.infra.gradle.asm.Bytecode.fromBytecodeLevel
import org.gradle.internal.impldep.org.objectweb.asm.ClassReader
import org.gradle.internal.impldep.org.objectweb.asm.ClassVisitor
import org.gradle.internal.impldep.org.objectweb.asm.Opcodes.ASM9
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.InputStream
import java.nio.file.Path

/**
 * # Classfile Utilities
 *
 * Parses well-formed JDK class bytecode, providing [ClassfileInfo] for introspection, usually during testing.
 */
public object Classfile {
  // Classfile visitor which extracts class information.
  public class ClassfileVisitor : ClassVisitor(ASM9) {
    private lateinit var name: String
    private var version: Int = -1
    private var target: JvmTarget? = null

    internal val className: String get() = name
    internal val classfileVersion: Int get() = version
    internal val classfileTarget: JvmTarget? get() = target

    override fun visit(
      version: Int,
      access: Int,
      name: String,
      signature: String?,
      superName: String?,
      interfaces: Array<out String>?,
    ) {
      this.version = version
      this.name = name
      this.target = JvmTarget.fromBytecodeLevel(version)
    }

    override fun toString(): String = "$name(version=$version, target=${target?.target ?: "unknown"})"
  }

  /**
   * ## Classfile Info
   *
   * Specifies information about a parsed classfile.
   */
  public class ClassfileInfo internal constructor (private val subject: Path, private val visitor: ClassfileVisitor) {
    /** Path to this class file. */
    public val path: Path get() = subject

    /** Name of this class. */
    public val name: String get() = visitor.className

    /** Name of this class. */
    public val qualifiedName: String get() = visitor.className.replace("/", ".")

    /** Bytecode version for this class. */
    public val version: Int get() = visitor.classfileVersion

    /** Bytecode version for this class, translated to a [JvmTarget]. */
    public val target: JvmTarget? get() = visitor.classfileTarget

    /** Fuzzy-match a name against this class. */
    public fun matches(token: String): Boolean = (
      name == token ||
      qualifiedName == token ||
      name.endsWith(token) ||
      qualifiedName.endsWith(token)
    )
  }

  /**
   * ## Parse Classfile
   *
   * Parse a classfile from a bytestream, and materialized [ClassfileInfo] about it.
   */
  @JvmStatic public fun parse(path: Path, data: InputStream): ClassfileInfo {
    val visitor = ClassfileVisitor()
    val reader = ClassReader(data)
    reader.accept(visitor, 0)
    return ClassfileInfo(path, visitor)
  }
}
