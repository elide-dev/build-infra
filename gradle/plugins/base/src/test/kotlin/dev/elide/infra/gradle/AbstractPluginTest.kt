package dev.elide.infra.gradle

import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException


enum class TestSourceFileLanguage {
  JAVA,
  KOTLIN,
}

@Suppress("unused") abstract class AbstractPluginTest {
  @TempDir lateinit var testProjectDir: File
  protected lateinit var sourcesRootJava: File
  protected lateinit var sourcesRootKotlin: File
  protected lateinit var settingsFile: File
  protected lateinit var buildFile: File

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

  fun StringBuilder.kotlin(@Language("kotlin") contents: String) {
    append(contents.trimIndent())
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

  @BeforeEach
  fun setup() {
    settingsFile = File(testProjectDir, "settings.gradle.kts")
    buildFile = File(testProjectDir, "build.gradle.kts")
    sourcesRootJava = testProjectDir.toPath().resolve("src/main/java").toFile()
    sourcesRootKotlin = testProjectDir.toPath().resolve("src/main/kotlin").toFile()
  }
}