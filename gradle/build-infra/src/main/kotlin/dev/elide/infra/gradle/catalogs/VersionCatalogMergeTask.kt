package dev.elide.infra.gradle.catalogs

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import org.tomlj.Toml
import org.tomlj.TomlArray
import org.tomlj.TomlParseResult
import org.tomlj.TomlTable
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.SortedMap
import java.util.SortedSet
import java.util.TreeMap
import java.util.TreeSet
import java.util.function.Supplier
import java.util.stream.Collectors
import javax.inject.Inject

// Retrieve a top-level catalog table.
private fun TomlParseResult.table(file: Path, name: String): TomlTable = requireNotNull(getTable(name)) {
  "Failed to resolve required `$name` section in catalog: '$file'"
}

// Append a TOML snippet for a version spec.
private fun StringBuilder.appendVersionSpec(spec: VersionCatalogMergeTask.VersionSpec) {
  val (ref, version) = when (val v = spec) {
    is VersionCatalogMergeTask.StringLiteralVersion -> false to v.version
    is VersionCatalogMergeTask.VersionReference -> true to v.ref
  }
  append(if (ref) { "version.ref = \"$version\"" } else { "version = \"$version\"" })
}

/**
 * # Task: Merge Version Catalogs
 *
 * Accepts multiple version catalogs as inputs, and deep-merges them, with later inputs overriding (so long as overrides
 * have been allowlisted).
 */
public abstract class VersionCatalogMergeTask @Inject constructor () : DefaultTask() {
  private object Constants {
    const val SECTION_VERSIONS: String = "versions"
    const val SECTION_LIBRARIES: String = "libraries"
    const val SECTION_PLUGINS: String = "plugins"
    const val SECTION_BUNDLES: String = "bundles"
  }

  // Version specification; either by reference or literal.
  internal sealed class VersionSpec (private val versionMap: Map<String, String>) {
    /**
     * Resolve the version provided by this version spec
     *
     * @param versions Version mappings
     * @return Supplier of the version
     */
    abstract fun resolve(versions: Map<String, String>): Supplier<String>

    /**
     * Resolve the version referenced by this object
     */
    val version: String get() = requireNotNull(resolve(versionMap).get()) {
      "Failed to resolve version reference"
    }

    companion object {
      @JvmStatic fun of(versionMap: Map<String, String>, key: String, value: Any?): VersionSpec = when (value) {
        null -> error("No version declared for target $key")
        is String -> StringLiteralVersion(versionMap, value)
        is TomlTable -> VersionReference(versionMap, requireNotNull(value["ref"] as? String))
        else -> error("Unrecognized type for version spec parse: '$value'")
      }
    }
  }

  // String literal version implementation.
  internal class StringLiteralVersion(map: Map<String, String>, private val literal: String) : VersionSpec(map) {
    override fun resolve(versions: Map<String, String>): Supplier<String> = Supplier { literal }
  }

  // Version reference implementation.
  internal class VersionReference(map: Map<String, String>, val ref: String): VersionSpec(map) {
    override fun resolve(versions: Map<String, String>): Supplier<String> = Supplier {
      requireNotNull(versions[ref]) { "Failed to resolve version reference $ref" }
    }
  }

  internal sealed interface CatalogEphemera {
    val key: String
  }

  // Version catalog entry for a library.
  internal data class LibraryMapping(
    override val key: String,
    @JvmField var module: String? = null,
    @JvmField var group: String? = null,
    @JvmField var name: String? = null,
    @JvmField var version: VersionSpec? = null,
  ) : CatalogEphemera {
    companion object {
      @JvmStatic fun from(
        versionMap: Map< String, String>,
        key: String,
        value: TomlTable,
      ): LibraryMapping = LibraryMapping(
        key = key,
        module = value["module"] as? String,
        group = value["group"] as? String,
        name = value["name"] as? String,
        version = VersionSpec.of(versionMap, key, value["version"]),
      ).also {
        require (
          it.module?.ifBlank { null } != null ||
          (it.group?.ifBlank { null } != null && it.name?.ifBlank { null } != null)
        ) {
          "One of `module` or `name` and `group` must be specified for library `$key`"
        }
      }
    }

    override fun toString(): String = StringBuilder().apply {
      append("$key = { ")
      when {
        !module?.ifBlank { null }.isNullOrBlank() -> {
          val mod = requireNotNull(module)
          append("module = \"$mod\", ")
        }

        !group?.ifBlank { null }.isNullOrBlank() -> {
          val group = requireNotNull(group)
          val name = requireNotNull(name)
          append("group = \"$group\", name = \"$name\", ")
        }
      }
      appendVersionSpec(requireNotNull(version))
      append(" }")
    }.toString()
  }

  // Version catalog entry for a plugin.
  internal data class PluginMapping(
    override val key: String,
    @JvmField var id: String? = null,
    @JvmField var version: VersionSpec? = null,
  ) : CatalogEphemera {
    companion object {
      @JvmStatic fun from(
        versionMap: Map<String, String>,
        key: String,
        value: TomlTable,
      ): PluginMapping = PluginMapping(
        key = key,
        id = requireNotNull(value["id"] as? String) { "No ID for plugin '$key'" },
        version = VersionSpec.of(versionMap, key, value["version"]),
      )
    }

    override fun toString(): String = StringBuilder().apply {
      append("$key = { ")
      append("id = \"$id\", ")
      appendVersionSpec(requireNotNull(version))
      append(" }")
    }.toString()
  }

  // Version catalog entry for a bundle.
  internal data class BundleMapping(
    override val key: String,
    @JvmField var libraries: SortedSet<String> = sortedSetOf(),
  ) : CatalogEphemera {
    companion object {
      @JvmStatic fun from(key: String, value: TomlArray): BundleMapping = BundleMapping(
        key = key,
        libraries = value.size().let {
          TreeSet<String>().apply {
            for (i in 0 until it) {
              add(value.getString(i))
            }
          }
        },
      )
    }

    override fun toString(): String = StringBuilder().apply {
      append("$key = [")
      append(libraries.joinToString(", ") { "\"$it\"" })
      append("]")
    }.toString()
  }

  // De-serialized Gradle Version Catalog file.
  internal data class VersionCatalog(
    @JvmField val path: Path? = null,
    @JvmField val versions: SortedMap<String, String> = sortedMapOf(),
    @JvmField val libraries: SortedMap<String, LibraryMapping> = sortedMapOf(),
    @JvmField val plugins: SortedMap<String, PluginMapping> = sortedMapOf(),
    @JvmField val bundles: SortedMap<String, BundleMapping> = sortedMapOf(),
  ) {
    companion object {
      @JvmStatic fun parseFrom(file: Path): VersionCatalog {
        val toml: TomlParseResult = Toml.parse(file)
        val versions = toml.table(file, Constants.SECTION_VERSIONS)
        val libraries = toml.table(file, Constants.SECTION_LIBRARIES)
        val plugins = toml.table(file, Constants.SECTION_PLUGINS)
        val bundles = toml.getTable(Constants.SECTION_BUNDLES)
        val versionsMap = TreeMap(versions.toMap().map { it.key to it.value as String }.toMap())

        return VersionCatalog(
          path = file,
          versions = versionsMap,
          libraries = TreeMap(libraries.toMap().map {
            it.key to LibraryMapping.from(versionsMap, it.key, it.value as TomlTable)
          }.toMap()),
          plugins = TreeMap(plugins.toMap().map {
            it.key to PluginMapping.from(versionsMap, it.key, it.value as TomlTable)
          }.toMap()),
          bundles = bundles?.let {
            TreeMap(it.toMap().map { pair ->
              pair.key to BundleMapping.from(pair.key, pair.value as TomlArray)
            }.toMap())
          } ?: sortedMapOf(),
        )
      }
    }
  }

  // Holds a merged version catalog; each is merged into this object, applying settings as we go.
  internal inner class MergedVersionCatalog(
    private val base: VersionCatalog = VersionCatalog(),
    private val overrideTokens: SortedSet<String> = overrides.get().toSortedSet(),
  ) {
    // Check if the provided `key` is eligible to be overridden.
    private fun eligibleForOverride(key: String): Boolean = key.lowercase().trim().let { candidate ->
      overrideTokens.any { key in candidate }
    }

    // Check if the provided `key` is eligible to be overridden.
    private fun eligibleForOverride(key: String, op: () -> String) {
      require(eligibleForOverride(key)) {
        op.invoke()
      }
    }

    // Require that a merged value is either not present in the authoritative map, or eligible for overrides.
    private fun requireNotPresentOrEligible(section: String, map: Map<String, *>, key: String) {
      if (key in map) eligibleForOverride(key) {
        val tokens = overrideTokens.joinToString(",")
        "Key '$key' in section '$section' is duplicate, and not eligible for overrides (\"$tokens\")"
      }
    }

    // Merge the provided version value into the catalog.
    private fun mergeVersion(key: String, version: String) {
      requireNotPresentOrEligible(Constants.SECTION_VERSIONS, base.versions, key)
      base.versions[key] = version
    }

    // Merge the provided library mapping into the catalog.
    private fun mergeLibrary(key: String, lib: LibraryMapping) {
      requireNotPresentOrEligible(Constants.SECTION_LIBRARIES, base.libraries, key)
      base.libraries[key] = when (val existing = base.libraries[key]) {
        null -> lib
        else -> existing.copy(
          module = lib.module ?: existing.module,
          group = lib.group ?: existing.group,
          name = lib.name ?: existing.name,
          version = lib.version ?: existing.version,
        )
      }
    }

    // Merge the provided plugin mapping into the catalog.
    private fun mergePlugin(key: String, plugin: PluginMapping) {
      requireNotPresentOrEligible(Constants.SECTION_PLUGINS, base.plugins, key)
      base.plugins[key] = when (val existing = base.plugins[key]) {
        null -> plugin
        else -> existing.copy(
          id = plugin.id ?: existing.id,
          version = plugin.version ?: existing.version,
        )
      }
    }

    // Merge the provided bundle mapping into the catalog.
    private fun mergeBundle(key: String, bundle: BundleMapping) {
      requireNotPresentOrEligible(Constants.SECTION_BUNDLES, base.bundles, key)
      base.bundles[key] = when (val existing = base.bundles[key]) {
        null -> bundle
        else -> existing.copy(
          libraries = existing.libraries.plus(bundle.libraries).toSortedSet(),
        )
      }
    }

    operator fun plusAssign(other: VersionCatalog) {
      other.versions.map { mergeVersion(it.key, it.value) }
      other.libraries.map { mergeLibrary(it.key, it.value) }
      other.plugins.map { mergePlugin(it.key, it.value) }
      other.bundles.map { mergeBundle(it.key, it.value) }
    }

    // Render the merged TOML file to a string builder.
    fun toToml() = StringBuilder().apply {
      appendLine("[versions]")
      base.versions.forEach { appendLine("${it.key} = \"${it.value}\"") }
      appendLine()
      appendLine("[plugins]")
      base.plugins.forEach { appendLine(it.value.toString()) }
      appendLine()
      appendLine("[libraries]")
      base.libraries.forEach { appendLine(it.value.toString()) }
      appendLine()
      appendLine("[bundles]")
      base.bundles.forEach { appendLine(it.value.toString()) }
    }
  }

  /**
   * ## Catalogs
   *
   * Version catalogs to use as inputs to the merge operation
   */
  @get:InputFiles @get:PathSensitive(PathSensitivity.RELATIVE) public abstract val catalogs: ConfigurableFileCollection

  /**
   * ## Destination File
   *
   * Target file, where the merged catalog should be written to
   */
  @get:OutputFile public abstract val destinationFile: RegularFileProperty

  /**
   * ## Overrides
   *
   * Strings to allowlist for overrides; when collisions are encountered, they are checked for any of these substrings.
   * Matches allow the override, failures halt the build.
   */
  @get:Input public abstract val overrides: ListProperty<String>

  @TaskAction internal fun merge() {
    val files = catalogs.files
    val parseErrors = ArrayList<Throwable>()
    val catalogs = ArrayList<VersionCatalog>(files.size)

    files.map {
      try {
        logger.lifecycle("Parsing catalog '${it.toPath()}'")
        VersionCatalog.parseFrom(it.toPath())
      } catch (err: Throwable) {
        parseErrors.add(err)
        null
      }
    }.filter {
      it != null
    }.forEach {
      catalogs.add(it!!)
    }

    // check parse errors
    if (parseErrors.isNotEmpty()) {
      val first = parseErrors.first()
      throw IllegalStateException("Failed to parse one or more input version catalogs: ${first.message}", first)
    }

    // with no parse errors, we can begin merging
    val merged = MergedVersionCatalog()
    catalogs.forEach {
      logger.lifecycle("Merging catalog '${it.path ?: "in_memory"}'")
      merged += it
    }

    // open the destination file and write the merged catalog
    val targetFile = destinationFile.get().asFile
    try {
      targetFile.parentFile.mkdirs()
      targetFile.outputStream().bufferedWriter(StandardCharsets.UTF_8).use {
        it.write(merged.toToml().toString())
      }
    } catch (err: Throwable) {
      logger.error("Failed to write merged version catalog: $err", err)
    }
  }
}
