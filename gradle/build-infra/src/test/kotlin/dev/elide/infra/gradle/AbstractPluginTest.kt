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

@file:Suppress("SameParameterValue")

package dev.elide.infra.gradle

import dev.elide.infra.gradle.asm.Bytecode.compatibleWith
import dev.elide.infra.gradle.asm.Classfile
import kotlinx.collections.immutable.toImmutableList
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.io.TempDir
import java.io.*
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.jar.Attributes
import java.util.jar.Manifest
import java.util.stream.Stream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.collections.HashMap
import kotlin.io.path.pathString
import kotlin.test.*


enum class TestSourceFileLanguage {
  JAVA,
  KOTLIN,
}

// Convert `some.class.Name` to `some/class/Name.class`, as a path.
private fun String.toQualifiedClassPath(): Path = split(".").let {
  Path.of(it.first(), *it.drop(1).dropLast(1).toTypedArray(), "${it.last()}.class")
}

// Convert a path into a zip fs path.
private fun Path.toZipPath(fs: FileSystem): Path = pathString.split("/").let {
  if (it.size > 1) {
    fs.getPath(it.first(), *it.drop(1).toTypedArray())
  } else {
    fs.getPath(it.first())
  }
}

@Suppress("unused") abstract class AbstractPluginTest {
  @TempDir lateinit var testProjectDir: File
  protected lateinit var sourcesRootJava: File
  protected lateinit var sourcesRootKotlin: File
  protected lateinit var settingsFile: File
  protected lateinit var buildFile: File
  protected lateinit var gradleProperties: File

  internal companion object {
    val fsCache: MutableMap<URI, FileSystem> = HashMap()
  }

  /** Assertions that can be made on any output path */
  interface OutputPathAssertions : Path {
    // ------- Assertions: Basic -------------------------------------------------------------------------------------
    val path: Path
    fun exists(): Boolean = Files.exists(path)
    fun readable(): Boolean = Files.exists(path) && Files.isReadable(path)
    fun writable(): Boolean = Files.exists(path) && Files.isWritable(path)
    fun nonEmpty(): Boolean = exists() && readable() && Files.size(path) > 0L
    fun fileSize(): Long = Files.size(path)
    fun extension(): String = "." + path.toString().substringAfterLast(".")
    fun text(): String = Files.readString(path)
    fun bytes(): ByteArray = Files.readAllBytes(path)
    fun lines(): Stream<String> = Files.readAllLines(path).stream()
    fun lineCount(): Long = Files.lines(path).count()
    fun isDirectory(): Boolean = Files.isDirectory(path)
    fun isRegularFile(): Boolean = Files.isRegularFile(path)

    // ------- Assertions: Existence ---------------------------------------------------------------------------------
    fun assertExists() { assertTrue(exists(), "file expected to exist at path $this not found") }
    fun assertExists(message: String) { assertTrue(exists(), message) }
    fun assertExists(message: () -> String) { assertTrue(exists(), message.invoke()) }

    // ------- Assertions: Permissions -------------------------------------------------------------------------------
    fun assertReadable() { assertTrue(readable(), "file expected to be readable at path $this, but wasn't") }
    fun assertReadable(message: String) { assertTrue(readable(), message) }
    fun assertReadable(message: () -> String) { assertTrue(readable(), message.invoke()) }
    fun assertWritable() { assertTrue(writable(), "file expected to be writable at path $this, but wasn't") }
    fun assertWritable(message: String) { assertTrue(writable(), message) }
    fun assertWritable(message: () -> String) { assertTrue(writable(), message.invoke()) }
    fun assertReadWritable() { assertReadable(); assertWritable() }
    fun assertReadWritable(message: String) { assertTrue(readable() && writable(), message) }
    fun assertReadWritable(message: () -> String) { assertTrue(readable() && writable(), message.invoke()) }

    // ------- Assertions: Contents ----------------------------------------------------------------------------------
    fun assertNonEmpty() { assertTrue(nonEmpty(), "file expected to be non-empty at $this, but was empty") }
    fun assertNonEmpty(message: String) { assertTrue(nonEmpty(), message) }
    fun assertNonEmpty(message: () -> String) { assertTrue(nonEmpty(), message.invoke()) }
    fun assertFileContains(value: String, ignoreCase: Boolean = false) {
      assertContains(text(), value, ignoreCase, "value expected within payload, but not found")
    }
    fun assertFileContains(value: String, message: String, ignoreCase: Boolean = false) {
      assertContains(text(), value, ignoreCase, message)
    }
    fun assertFileContains(value: String, ignoreCase: Boolean = false, message: () -> String) {
      assertContains(text(), value, ignoreCase, message.invoke())
    }

    // ------- Assertions: Extension ---------------------------------------------------------------------------------
    fun assertExtension(ext: String) {
      assertEquals(ext, extension(), "expected extension '$ext' for file $this, but got ${extension()}")
    }
    fun assertExtension(ext: String, message: String) { assertEquals(ext, extension(), message) }
    fun assertExtension(ext: String, message: () -> String) { assertEquals(ext, extension(), message.toString()) }

    // ------- Assertions: Directories -------------------------------------------------------------------------------
    fun assertDirectory(assertions: DirectoryAssertionsDsl.() -> Unit = {}) {
      DirectoryAssertionsDsl(this).invoke(assertions)
    }

    // ------- Assertions: Zip Files ---------------------------------------------------------------------------------
    fun assertZip(assertions: ZipAssertionsDsl.() -> Unit = {}) {
      assertNonEmpty()
      assertReadable()

      val data = assertDoesNotThrow { Files.readAllBytes(path) }
      val fsUri = URI.create(path.toAbsolutePath().toUri().toString().replace("file:///", "jar:file:/"))
      val zipfs: FileSystem = fsCache[fsUri] ?: FileSystems.newFileSystem(
        URI.create(path.toAbsolutePath().toUri().toString().replace("file:///", "jar:file:/")),
        mapOf("create" to "false"),
      ).also {
        fsCache[fsUri] = it
      }
      ZipInputStream(ByteArrayInputStream(data)).use {
        assertions.invoke(ZipAssertionsDsl(it, zipfs, path))
      }
    }

    // ------- Assertions: Jar Files ---------------------------------------------------------------------------------
    fun assertJar(assertions: JarAssertionsDsl.() -> Unit = {}) = assertZip {
      JarAssertionsDsl(this).invoke(assertions)
    }

    // ------- Assertions: JMod Files --------------------------------------------------------------------------------
    fun assertJMod(assertions: JmodAssertionsDsl.() -> Unit = {}) = assertZip {
      JmodAssertionsDsl(this).invoke(assertions)
    }

    // ------- Assertions: JLink Outputs -----------------------------------------------------------------------------
    fun assertJLink(assertions: JlinkAssertionsDsl.() -> Unit = {}) {
      JlinkAssertionsDsl(this).invoke(assertions)
    }

    // ------- Assertions: Native Binaries ---------------------------------------------------------------------------
    fun assertNative(assertions: NativeBinaryAssertionsDsl.() -> Unit = {}) {
      NativeBinaryAssertionsDsl(this).invoke(assertions)
    }
  }

  /** Assertions on archives or directories; generic containers of files */
  interface FileContainerAssertions {
    /** Get the list of file paths constituent to this container */
    fun files(): List<Path>

    /** Get the count of files in this directory or archive */
    fun fileCount(): Long = files().size.toLong()

    /** Assert that the member count is non-zero */
    fun hasFiles(): Boolean = fileCount() > 0L

    /** Assert that a file container has a file at a given path */
    fun hasFile(path: String): Boolean = hasFile(Path.of(path))

    /** Assert that a file container has a file at a given path */
    fun hasFile(first: String, vararg segments: String): Boolean = hasFile(Path.of(first, *segments))

    /** Assert that a file container has a file at a given path */
    fun hasFile(path: Path): Boolean = files().contains(path)
  }

  /** Assertions that can be made on any output directory path */
  interface OutputDirectoryAssertions : FileContainerAssertions, OutputPathAssertions {
    /** Check a directory for emptiness */
    fun directoryFileCount(): Long

    fun directoryHasFiles(): Boolean = directoryFileCount() > 0L
    fun assertDirectoryNonEmpty() = assertTrue(
      directoryHasFiles(),
      "directory '$this' expected to have files, but does not"
    )

    fun assertDirectoryNonEmpty(message: String) = assertTrue(directoryHasFiles(), message)
    fun assertDirectoryNonEmpty(message: () -> String) = assertTrue(directoryHasFiles(), message.invoke())
  }

  /** Assertions that can be made on any output zip file (or zip-like file) */
  interface ZipAssertions : OutputPathAssertions, FileContainerAssertions

  /** Assertions against a classfile, regardless of where it originates from */
  interface ClassfileAssertions : OutputPathAssertions {
    /** Check that the file at the provided [path] is a valid classfile */
    fun validClassfile(): Boolean

    /** Check that the file at the provided [path] is a valid classfile for the provided JVM [target] */
    fun validClassfileFor(target: JvmTarget): Boolean

    /** Obtain the bytecode target for this classfile */
    fun bytecodeTarget(): JvmTarget

    /** Assert that the file at the provided [path] is a valid classfile */
    fun assertClassfile() = assertTrue(validClassfile(), "expected $this to be a valid classfile")

    /** Assert that the file at the provided [path] is a valid classfile */
    fun assertClassfile(message: String) = assertTrue(validClassfile(), message)

    /** Assert that the file at the provided [path] is a valid classfile */
    fun assertClassfile(message: () -> String) = assertTrue(validClassfile(), message.invoke())

    /** Assert that a valid classfile exists at the provided [qualified] class name */
    fun assertClass(qualified: String) = assertTrue(
      qualified.toQualifiedClassPath().let { path == it },
      "expected $this to match qualified class path '$qualified'"
    )

    /** Assert that a valid classfile exists at the provided [qualified] class name */
    fun assertClass(qualified: String, message: String) = assertTrue(
      qualified.toQualifiedClassPath().let { path == it },
      message,
    )

    /** Assert that a valid classfile exists at the provided [qualified] class name */
    fun assertClass(qualified: String, message: () -> String) = assertTrue(
      qualified.toQualifiedClassPath().let { path == it },
      message.invoke(),
    )

    /** Assert that this classfile is valid at the provided JVM target */
    fun assertLoadsAt(jvmTarget: JvmTarget) = assertTrue(
      validClassfileFor(jvmTarget),
      "expected '$this' to load at target JVM ${jvmTarget.target}, but does not"
    )

    /** Assert that this classfile is valid at the provided JVM target */
    fun assertLoadsAt(jvmTarget: JvmTarget, message: String) = assertTrue(
      validClassfileFor(jvmTarget),
      message,
    )

    /** Assert that this classfile is valid at the provided JVM target */
    fun assertLoadsAt(jvmTarget: JvmTarget, message: () -> String) = assertTrue(
      validClassfileFor(jvmTarget),
      message.invoke(),
    )

    /** Assert that this classfile contains bytecode at the provided target */
    fun assertBytecodeTarget(jvmTarget: JvmTarget) = assertEquals(
      jvmTarget,
      bytecodeTarget(),
      "expected bytecode target '${jvmTarget.target}', but got ${bytecodeTarget().target}",
    )

    /** Assert that this classfile contains bytecode at the provided target */
    fun assertBytecodeTarget(jvmTarget: JvmTarget, message: String) = assertEquals(
      jvmTarget,
      bytecodeTarget(),
      message,
    )

    /** Assert that this classfile contains bytecode at the provided target */
    fun assertBytecodeTarget(jvmTarget: JvmTarget, message: () -> String) = assertEquals(
      jvmTarget,
      bytecodeTarget(),
      message.invoke(),
    )
  }

  /** Assertions against a container class files, regardless of where they originate from */
  interface ClassfileContainerAssertions : OutputPathAssertions, FileContainerAssertions {
    /** Check that the file at the provided [path] is a valid classfile */
    fun validClass(path: Path): Boolean

    /** Check that the file at the provided [qualified] name is a valid classfile */
    fun validClass(qualified: String): Boolean

    /** Check that the file at the provided [path] is a valid class for the provided JVM [target] */
    fun validClassFor(target: JvmTarget, path: Path): Boolean

    /** Check that the file at the provided [path] is a valid class for the provided JVM [target] */
    fun validClassFor(target: JvmTarget, qualified: String): Boolean

    /** Obtain the bytecode target for the classfile at the provided path */
    fun bytecodeTarget(path: Path): JvmTarget

    /** Obtain the bytecode target for the qualified class */
    fun bytecodeTarget(qualified: String): JvmTarget


    /** Assert that the file at the provided [path] is a valid classfile */
    fun assertClass(path: Path) = assertTrue(
      validClass(path),
      "expected $this to be a classfile"
    )

    /** Assert that the file at the provided [path] is a valid classfile */
    fun assertClass(path: Path, message: String) = assertTrue(
      validClass(path),
      message,
    )

    /** Assert that the file at the provided [path] is a valid classfile */
    fun assertClass(path: Path, message: () -> String) = assertTrue(
      validClass(path),
      message.invoke(),
    )

    /** Assert that the file at the provided [qualified] name is a valid classfile */
    fun assertClass(qualified: String) = assertTrue(
      validClass(qualified),
      "expected '$qualified' to exist within path '$this'",
    )

    /** Assert that the file at the provided [qualified] name is a valid classfile */
    fun assertClass(qualified: String, message: String) = assertTrue(
      validClass(qualified),
      message,
    )

    /** Assert that the file at the provided [qualified] name is a valid classfile */
    fun assertClass(qualified: String, message: () -> String) = assertTrue(
      validClass(qualified),
      message.invoke(),
    )

    /** Assert that this class is valid at the provided JVM target */
    fun assertLoadsAt(jvmTarget: JvmTarget, path: Path) = assertTrue(
      validClass(path) && validClassFor(jvmTarget, path),
      "expected '$this' to be a valid class at target '${jvmTarget.target}"
    )

    /** Assert that this class is valid at the provided JVM target */
    fun assertLoadsAt(jvmTarget: JvmTarget, path: Path, message: String) = assertTrue(
      validClass(path) && validClassFor(jvmTarget, path),
      message,
    )

    /** Assert that this class is valid at the provided JVM target */
    fun assertLoadsAt(jvmTarget: JvmTarget, path: Path, message: () -> String) = assertTrue(
      validClass(path) && validClassFor(jvmTarget, path),
      message.invoke(),
    )

    /** Assert that this class is valid at the provided JVM target */
    fun assertLoadsAt(jvmTarget: JvmTarget, qualified: String) = assertTrue(
      validClass(qualified) && validClassFor(jvmTarget, qualified),
      "expected '$this' to contain class '$qualified', valid for jvm target '${jvmTarget.target}'",
    )

    /** Assert that this class is valid at the provided JVM target */
    fun assertLoadsAt(jvmTarget: JvmTarget, qualified: String, message: String) = assertTrue(
      validClass(qualified) && validClassFor(jvmTarget, qualified),
      message,
    )

    /** Assert that this class is valid at the provided JVM target */
    fun assertLoadsAt(jvmTarget: JvmTarget, qualified: String, message: () -> String) = assertTrue(
      validClass(qualified) && validClassFor(jvmTarget, qualified),
      message.invoke(),
    )

    /** Assert that this class contains bytecode at the provided target */
    fun assertBytecodeTarget(jvmTarget: JvmTarget, path: Path) = assertEquals(
      jvmTarget,
      bytecodeTarget(path),
      "expected bytecode target '${jvmTarget.target}' for path '$path'"
    )

    /** Assert that this class contains bytecode at the provided target */
    fun assertBytecodeTarget(jvmTarget: JvmTarget, path: Path, message: String) = assertEquals(
      jvmTarget,
      bytecodeTarget(path),
      message,
    )

    /** Assert that this class contains bytecode at the provided target */
    fun assertBytecodeTarget(jvmTarget: JvmTarget, path: Path, message: () -> String) = assertEquals(
      jvmTarget,
      bytecodeTarget(path),
      message.invoke(),
    )

    /** Assert that this class contains bytecode at the provided target */
    fun assertBytecodeTarget(jvmTarget: JvmTarget, qualified: String) = assertEquals(
      jvmTarget,
      bytecodeTarget(qualified),
      "expected bytecode target '${jvmTarget.target}' for class '${qualified}'"
    )

    /** Assert that this class contains bytecode at the provided target */
    fun assertBytecodeTarget(jvmTarget: JvmTarget, qualified: String, message: String) = assertEquals(
      jvmTarget,
      bytecodeTarget(qualified),
      message,
    )

    /** Assert that this class contains bytecode at the provided target */
    fun assertBytecodeTarget(jvmTarget: JvmTarget, qualified: String, message: () -> String) = assertEquals(
      jvmTarget,
      bytecodeTarget(qualified),
      message.invoke(),
    )
  }

  /** Assertions that can be made on any output JAR file */
  interface JarAssertions : ClassfileContainerAssertions, ZipAssertions {
    /** Get the count of class files in this directory or archive */
    fun classCount(): Int

    /** Get the count of resource files in this directory or archive */
    fun resourceCount(): Int

    /** Retrieve the list of paths which provide built class files */
    fun classes(): List<Classfile.ClassfileInfo>

    /** Retrieve the list of paths which are classes that are compatible with the provided [target] */
    fun classesFor(target: JvmTarget): List<Classfile.ClassfileInfo>

    /** Retrieve the list of paths which are classes that are specifically built at a given JVM target. */
    fun classesFor(target: JvmTarget, strict: Boolean): List<Classfile.ClassfileInfo>

    /** Retrieve the list of paths which provide non-class-files */
    fun resources(): List<Path>

    /** Retrieve the standard manifest for this JAR */
    fun manifest(): Map<String, Any>

    /** Assert that a file container has a class at a given well-qualified name */
    fun hasClass(name: String): Boolean

    /** Assert that a file container has a class at a given well-qualified name, at the provided target (or below) */
    fun hasClassFor(target: JvmTarget, name: String): Boolean

    /** Assert that a file container has a class at a given well-qualified name, at the provided target (strictly) */
    fun hasClassAtExactly(target: JvmTarget, name: String): Boolean

    /** Assert that a file container has a class at a given well-qualified name, and within a path prefix */
    fun hasClassWithin(prefix: String, name: String): Boolean

    /** Assert that a file container has a class at a given well-qualified name, and within a path prefix */
    fun hasClassWithin(target: JvmTarget, prefix: String, name: String): Boolean

    /** Indicate whether this JAR has a module definition */
    fun isModular(): Boolean = hasFile("module-info.class")

    /** Indicate whether this JAR has a `Multi-Release` flag in its JAR manifest */
    fun isMultiRelease(): Boolean = manifest().let {
      it.containsKey("Multi-Release") && it["Multi-Release"] == "true"
    }
  }

  /** Assertions that can be made on any output JMod file */
  interface JmodAssertions : ZipAssertions {
    //
  }

  /** Assertions that can be made on any output JLink output directory */
  interface JlinkAssertions : OutputDirectoryAssertions {
    //
  }

  /** Assertions that can be made on any output native binary */
  interface NativeBinaryAssertions : OutputPathAssertions {
    //
  }

  sealed class AbstractSpecializedAssertionsDsl(private val outpath: Path) : Path by outpath, OutputPathAssertions {
    override val path: Path get() = outpath
    override fun toString(): String = outpath.toString()

    open fun baseline() {
      /* No baseline assertions applied. */
    }

    inline operator fun <reified C: AbstractSpecializedAssertionsDsl> invoke(operation: C.() -> Unit) {
      baseline()
      operation.invoke(this as C)
    }
  }

  sealed class AbstractDirectoryAssertionsDsl(outpath: Path) :
    OutputDirectoryAssertions,
    AbstractSpecializedAssertionsDsl(outpath) {
    private val fileSet: SortedSet<Path> by lazy { files().toSortedSet() }
    override fun directoryFileCount(): Long = Files.list(path).count()
    override fun fileCount(): Long = directoryFileCount()
    override fun files(): List<Path> = Files.list(path).toList()
    override fun hasFile(path: String): Boolean = fileSet.any { it.toString() == path }
    override fun hasFile(first: String, vararg segments: String): Boolean = hasFile(Path.of(first, *segments))
    override fun hasFile(path: Path): Boolean = path in fileSet
  }

  open class ZipAssertionsDsl(
    internal val zipstream: ZipInputStream,
    internal val zipfs: FileSystem,
    outpath: Path
  ) : ZipAssertions,
      AbstractSpecializedAssertionsDsl(outpath) {
    protected val fileSet: SortedSet<Path> by lazy { files().toSortedSet() }
    protected val entries: List<ZipEntry> by lazy {
      val agg = LinkedList<ZipEntry>()
      var current = zipstream.nextEntry
      while (current != null) {
        agg.add(current)
        current = zipstream.nextEntry
      }
      agg
    }

    override fun fileCount(): Long = entries.size.toLong()
    override fun files(): List<Path> = entries.map {
      Path.of(it.name)
    }

    override fun hasFile(path: String): Boolean = entries.any {
      it.name == path || Path.of(it.name).toString() == path
    }

    override fun hasFile(first: String, vararg segments: String): Boolean = Path.of(first, *segments).let { subject ->
      entries.any {
        it.name == listOf(first).plus(segments).joinToString("/") || Path.of(it.name) == subject
      }
    }

    override fun hasFile(path: Path): Boolean = entries.any {
      it.name == path.toString() || Path.of(it.name) == path
    }
  }

  class JarAssertionsDsl(zipstream: ZipInputStream, zipfs: FileSystem, outpath: Path) :
    JarAssertions,
    ZipAssertionsDsl(zipstream, zipfs, outpath) {
    //
    constructor(other: ZipAssertionsDsl) : this(other.zipstream, other.zipfs, other.path)

    private val parsedClasses by lazy {
      files().filter {
        val pathString = it.pathString

        // must be a `.class` file...
        pathString.endsWith(".class") &&
          // and not be a resource (but `META-INF/versions/` is allowed)
          (!pathString.startsWith("META-INF") || pathString.startsWith("META-INF/versions/"))
      }.map {
        decodeClassfile(it.toZipPath(zipfs))
      }.toImmutableList()
    }

    // -- JAR Processing

    // Obtain a zip entry for the `path` or fail.
    private fun entryForPath(path: Path): ZipEntry = entries.find {
      it.name == path.toString()
    } ?: error("No zip entry for path '$path' in file '$this'")

    // Obtain a new input stream for a given `path`.
    private fun streamForPath(path: Path): InputStream = entryForPath(path).let {
      zipfs.provider().newInputStream(path.toZipPath(zipfs))
    }

    // Decode a classfile at the provided `path, at the highest bytecode level or the provided level.
    private fun decodeClassfile(path: Path): Classfile.ClassfileInfo = Classfile.parse(
      path,
      streamForPath(path),
    )

    override fun validClass(path: Path): Boolean = try {
      decodeClassfile(path)
      true
    } catch (thr: Throwable) {
      false
    }

    override fun validClass(qualified: String): Boolean = qualified.toQualifiedClassPath().let { path ->
      validClass(path)
    }

    override fun validClassFor(target: JvmTarget, path: Path): Boolean = decodeClassfile(path).let { classfile ->
      val subject = classfile.target
      subject != null && subject <= target
    }

    override fun validClassFor(target: JvmTarget, qualified: String): Boolean = qualified.toQualifiedClassPath().let {
      validClassFor(target, it)
    }

    override fun bytecodeTarget(path: Path): JvmTarget = decodeClassfile(path)
      .target
      ?: error("Failed to resolve bytecode target for class at path '$path'")

    override fun bytecodeTarget(qualified: String): JvmTarget =
      bytecodeTarget(qualified.toQualifiedClassPath())

    // -- JAR Accessors

    override fun classCount(): Int = classes().count()

    override fun resourceCount(): Int = resources().count()

    override fun classes(): List<Classfile.ClassfileInfo> = parsedClasses

    override fun classesFor(target: JvmTarget): List<Classfile.ClassfileInfo> = classesFor(
      target,
      strict = false,
    )

    override fun classesFor(target: JvmTarget, strict: Boolean): List<Classfile.ClassfileInfo> = classes().filter {
      val subj = it.target
      subj != null && when (strict) {
        true -> subj == target
        false -> subj.compatibleWith(target)
      }
    }

    override fun resources(): List<Path> = files().filter {
      !it.endsWith(".class")
    }

    override fun manifest(): Map<String, Any> = streamForPath(Path.of("META-INF", "MANIFEST.MF")).let {
      val manifest = Manifest(it)
      val attr: Attributes = manifest.mainAttributes
      return attr.map { entry -> (entry.key as Attributes.Name).toString() to entry.value }.toMap()
    }

    // -- JAR Class Membership Checks

    override fun hasClass(name: String): Boolean = parsedClasses.find {
      it.matches(name)
    } != null

    override fun hasClassFor(target: JvmTarget, name: String): Boolean = parsedClasses.find {
      it.matches(name) && requireNotNull(it.target).compatibleWith(target)
    } != null

    override fun hasClassAtExactly(target: JvmTarget, name: String): Boolean = parsedClasses.find {
      it.matches(name) && it.target == target
    } != null

    override fun hasClassWithin(prefix: String, name: String): Boolean = parsedClasses.find {
      it.matches(name) && it.path.startsWith(prefix)
    } != null

    override fun hasClassWithin(target: JvmTarget, prefix: String, name: String): Boolean = parsedClasses.find {
      it.matches(name) && it.path.startsWith(prefix) && it.target == target
    } != null
  }

  class JmodAssertionsDsl(zipstream: ZipInputStream, zipfs: FileSystem, outpath: Path) :
    JmodAssertions,
    ZipAssertionsDsl(zipstream, zipfs, outpath) {
    //
    constructor(other: ZipAssertionsDsl) : this(other.zipstream, other.zipfs, other.path)
  }

  class DirectoryAssertionsDsl(outpath: Path) : OutputDirectoryAssertions, AbstractDirectoryAssertionsDsl(outpath)

  class JlinkAssertionsDsl(outpath: Path) : JlinkAssertions, AbstractDirectoryAssertionsDsl(outpath) {
    //
  }

  class NativeBinaryAssertionsDsl(outpath: Path) : NativeBinaryAssertions, AbstractSpecializedAssertionsDsl(outpath) {
    //
  }

  class OutputPathAssertionsDsl(val outpath: Path) : Path by outpath, OutputPathAssertions {
    override val path: Path get() = outpath
    override fun toString(): String = outpath.toString()
  }

  protected fun outputPath(path: String): Path {
    return testProjectDir.toPath().resolve("build").resolve(path)
  }

  protected fun outputPath(first: String, vararg more: String): Path {
    return testProjectDir.toPath().resolve("build").resolve(Path.of(first, *more))
  }

  protected fun outputPath(path: String, assertions: OutputPathAssertions.() -> Unit): Path {
    return OutputPathAssertionsDsl(outputPath(path)).let {
      it.apply(assertions)
      it.outpath
    }
  }

  protected fun outputPath(path: String, vararg more: String, assertions: OutputPathAssertionsDsl.() -> Unit): Path {
    return OutputPathAssertionsDsl(outputPath(path, *more)).let {
      it.apply(assertions)
      it.outpath
    }
  }

  protected fun writeSourceFile(language: TestSourceFileLanguage, path: String, contents: StringBuilder) {
    val sourceRoot = when (language) {
      TestSourceFileLanguage.KOTLIN -> sourcesRootKotlin
      TestSourceFileLanguage.JAVA -> sourcesRootJava
    }
    val target = sourceRoot.toPath().resolve(path).toFile()
    target.parentFile.mkdirs()
    writeFile(target, contents.toString())
  }

  protected fun writeSourceFile(language: TestSourceFileLanguage, path: String, producer: StringBuilder.() -> Unit) {
    writeSourceFile(language, path, StringBuilder().apply(producer))
  }

  fun StringBuilder.java(@Language("java") contents: String) {
    append(contents.trimIndent())
  }

  fun StringBuilder.gradleKts(contents: String) {
    append(contents.trimIndent())
  }

  fun StringBuilder.properties(@Language("properties") contents: String) {
    append(contents.trimIndent())
  }

  fun StringBuilder.kotlin(@Language("kotlin") contents: String) {
    append(contents.trimIndent())
  }

  @Throws(IOException::class)
  protected fun writeFile(destination: File, content: StringBuilder) {
    writeFile(destination, content.toString())
  }

  @Throws(IOException::class)
  protected fun writeFile(destination: File, content: StringBuilder.() -> Unit) {
    writeFile(destination, StringBuilder().apply(content))
  }

  @Throws(IOException::class)
  protected fun writeFile(destination: File, content: String) {
    var output: BufferedWriter? = null
    try {
      output = BufferedWriter(FileWriter(destination))
      output.write(content)
    } finally {
      output?.close()
    }
  }

  @BeforeEach fun setup() {
    settingsFile = File(testProjectDir, "settings.gradle.kts")
    buildFile = File(testProjectDir, "build.gradle.kts")
    gradleProperties = File(testProjectDir, "gradle.properties")
    sourcesRootJava = testProjectDir.toPath().resolve("src/main/java").toFile()
    sourcesRootKotlin = testProjectDir.toPath().resolve("src/main/kotlin").toFile()

    // defaults: settings file
    writeFile(settingsFile) {
      gradleKts("""
        rootProject.name = "hello-world"
      """)
    }

    // defaults: gradle properties
    writeFile(gradleProperties) {
      properties("""
        # gradle properties
      """)
    }
  }

  @AfterEach fun reset() {
    fsCache.values.forEach {
      try {
        it.close()
      } catch (err: Throwable) {
        // no-op
      }
    }
    fsCache.clear()
  }
}