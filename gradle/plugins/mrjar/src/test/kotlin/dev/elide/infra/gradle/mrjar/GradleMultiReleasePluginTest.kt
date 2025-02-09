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
@file:Suppress(
  "LongMethod",
  "LargeClass",
  "ForbiddenComment",
)

package dev.elide.infra.gradle.mrjar

import dev.elide.infra.gradle.AbstractPluginTest
import dev.elide.infra.gradle.BuildConstants
import dev.elide.infra.gradle.TestSourceFileLanguage
import dev.elide.infra.gradle.jpms.Java9Modularity
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import kotlin.test.*

private val defaultBuildKts: String = """
  plugins {
    java
    `java-library`
    id("dev.elide.mrjar")
  }
""".trimIndent()

private val defaultModuleJava: String = """
  module sample.test {
    requires java.base;
  }
""".trimIndent()

private val defaultEntryKotlin: String = """
package dev.elide

object Sample {
  const val sample = "SAMPLE"
}
""".trimIndent()

private val defaultEntryJava: String = """
package dev.elide;

public final class Sample {
  public final static String sample = "SAMPLE";
}
""".trimIndent()

class GradleMultiReleasePluginTest : AbstractPluginTest() {
  private interface ProjectSpawnApi {
    var modular: Boolean
    var kotlin: Boolean
    var buildGradleKts: String
    var moduleJava: String
    var sources: MutableMap<String, Pair<TestSourceFileLanguage, String>>
    val properties: MutableMap<String, Any?>
    fun test(cbk: () -> Unit)
  }

  private inline fun <R> withProject(
    enableJpms: Boolean = false,
    enableKotlin: Boolean = false,
    crossinline cfg: ProjectSpawnApi.() -> R,
  ): ProjectSpawnApi {
    val srcMap = mutableMapOf<String, Pair<TestSourceFileLanguage, String>>()
    when (enableKotlin) {
      true -> srcMap["dev/elide/Sample.kt"] = TestSourceFileLanguage.KOTLIN to defaultEntryKotlin
      false -> srcMap["dev/elide/Sample.java"] = TestSourceFileLanguage.JAVA to defaultEntryJava
    }

    val props = mutableMapOf<String, Any?>()
    val base = object: ProjectSpawnApi {
      override var kotlin: Boolean = enableKotlin
      override var modular: Boolean = enableJpms
      override var moduleJava = defaultModuleJava
      override var buildGradleKts = defaultBuildKts
      override var sources = srcMap
      override val properties = props

      override fun test(cbk: () -> Unit) {
        writeFile(buildFile) {
          gradleKts(buildGradleKts)
        }

        if (this.modular) {
          writeSourceFile(TestSourceFileLanguage.JAVA, "module-info.java") {
            java(moduleJava)
          }
        }

        sources.forEach { entry ->
          val (lang, content) = entry.value

          writeSourceFile(lang, entry.key) {
            when (lang) {
              TestSourceFileLanguage.JAVA -> java(content)
              TestSourceFileLanguage.KOTLIN -> kotlin(content)
            }
          }
        }

        writeFile(gradleProperties) {
          properties(StringBuilder().apply {
            props.forEach { (k, v) ->
              appendLine("$k=$v")
            }
          }.toString())
        }
        cbk.invoke()
      }
    }
    return base.also { base.cfg() }
  }

  private fun withDefaultProject(): ProjectSpawnApi = withProject { /* nothing */ }

  private fun runAndAssert(vararg args: String, assertions: BuildResult.() -> Unit): BuildResult {
    val result: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments(*(args.ifEmpty { arrayOf("build") }))
      .withPluginClasspath()
      .build()

    assertions.invoke(result)
    return result
  }

  @Test fun `apply mrjar plugin and query tasks`() = withDefaultProject().test {
    runAndAssert("tasks", "--stacktrace") {
      assertEquals(TaskOutcome.SUCCESS, task(":tasks")?.outcome)
    }
  }

  @Test fun `optimized mrjar should appear as task option (non-modular)`() = withProject {
    modular = false
    properties["conventions.jvm.minimum"] = "11"
  }.test {
    runAndAssert("tasks") {
      assertEquals(TaskOutcome.SUCCESS, task(":tasks")?.outcome)
      assertContains(output, Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)
    }
  }

  @Test fun `optimized mrjar should appear as task option (modular)`() = withProject {
    modular = true
    properties["conventions.jvm.minimum"] = "11"
  }.test {
    runAndAssert("tasks") {
      assertEquals(TaskOutcome.SUCCESS, task(":tasks")?.outcome)
      assertTrue(output.contains(Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR))
    }
  }

  @Test fun `build modular mrjar with jvm minimum target via properties`() = withProject {
    modular = true
    properties["conventions.jvm.minimum"] = "11"
  }.test {
    runAndAssert("build") {
      assertNotNull(
        task(":" + BuildConstants.TaskName.COMPILE_JAVA),
        "sanity: java compile task should be present in graph",
      )
      assertEquals(
        TaskOutcome.SUCCESS,
        task(":" + BuildConstants.TaskName.COMPILE_JAVA)?.outcome,
        "sanity: java compile task should be successful",
      )
      assertNotNull(
        task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR),
        "merged mrjar task '${Java9Modularity.Constants.TaskName.MERGED_MR_JAR}' should be present",
      )
      assertEquals(
        TaskOutcome.SUCCESS,
        task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR)?.outcome,
        "merged mr jar task should be successful",
      )
      assertNotNull(
        task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR),
        "optimized mrjar task '${Java9Modularity.Constants.TaskName.MERGED_MR_JAR}' should be present",
      )
      assertEquals(
        TaskOutcome.SUCCESS,
        task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)?.outcome,
        "optimized mr jar task should be successful",
      )
      assertEquals(TaskOutcome.SUCCESS, task(":build")?.outcome)

      // check outputs
      listOf(
        "hello-world",
        "hello-world-jvm11",
        "hello-world-jvm17",
        "hello-world-jvm21",
        "hello-world-multirelease-all",
        "hello-world-multirelease",
      ).forEach { jar ->
        outputPath("libs", "$jar.jar") {
          assertExtension(".jar")
          assertReadable()
          assertNonEmpty()
          assertJar {
            assertTrue(classes().isNotEmpty())
            assertTrue(hasClass("dev.elide.Sample"))
          }
        }
      }

      // MRJAR should have all target classes and the module info

      outputPath("libs", "hello-world-multirelease.jar") {
        assertJar {
          assertTrue(hasClass("module-info"))
          assertContains(classesFor(JvmTarget.JVM_21).map { it.qualifiedName }, "dev.elide.Sample")
          assertContains(classesFor(JvmTarget.JVM_17).map { it.qualifiedName }, "dev.elide.Sample")
          assertContains(classesFor(JvmTarget.JVM_11).map { it.qualifiedName }, "dev.elide.Sample")
          assertTrue("Multi-Release" in manifest())
          assertTrue(isModular())
          assertTrue(isMultiRelease())
          assertFalse(hasClass("some.other.ClassName"))
        }
      }

      // thin jars should only have their target classes

      outputPath("libs", "hello-world-jvm11.jar") {
        assertJar {
          assertContains(classesFor(JvmTarget.JVM_11).map { it.qualifiedName }, "dev.elide.Sample")
          assertFalse(
            hasClassFor(JvmTarget.JVM_21, "dev.elide.Sample"),
            "jvm11 tareget should not have jvm21",
          )
          assertFalse(
            hasClassFor(JvmTarget.JVM_17, "dev.elide.Sample"),
            "jvm11 tareget should not have jvm17",
          )
          assertFalse(hasClass("some.other.ClassName"))
        }
      }

      outputPath("libs", "hello-world-jvm17.jar") {
        assertJar {
          assertContains(classesFor(JvmTarget.JVM_17).map { it.qualifiedName }, "dev.elide.Sample")
          assertFalse(hasClass("some.other.ClassName"))
        }
      }

      outputPath("libs", "hello-world-jvm21.jar") {
        assertJar {
          assertContains(classesFor(JvmTarget.JVM_21).map { it.qualifiedName }, "dev.elide.Sample")
          assertFalse(hasClass("some.other.ClassName"))
        }
      }

      // the user's jar should be unmodified in this operating mode

      outputPath("libs", "hello-world.jar") {
        assertJar {
          assertContains(classesFor(JvmTarget.JVM_21).map { it.qualifiedName }, "dev.elide.Sample")
          assertFalse(
            hasClass("some.other.ClassName"),
            "jar target should not have made-up class",
          )
        }
      }
    }
  }

  @Test fun `build non-mrjar with mrjar plugin applied (modular)`() = withProject { modular = true }.test {
    runAndAssert("jar") {
      val jar = task(":jar")
      assertEquals(TaskOutcome.SUCCESS, jar?.outcome)
      val mergedJarTask = task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR)
      val optimizedJarTask = task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)
      assertNull(mergedJarTask, "merged jar task should not be in task graph by default")
      assertNull(optimizedJarTask, "optimized jar task should not be in task graph by default")
    }
  }

  @Test fun `build non-mrjar with mrjar plugin applied (non-modular)`() = withDefaultProject().test {
    runAndAssert("jar") {
      val jar = task(":jar")
      assertEquals(TaskOutcome.SUCCESS, jar?.outcome)
      val mergedJarTask = task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR)
      val optimizedJarTask = task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)
      assertNull(mergedJarTask, "merged jar task should not be in task graph by default")
      assertNull(optimizedJarTask, "optimized jar task should not be in task graph by default")
    }
  }

  @Test fun `build non-mrjar with mrjar plugin applied and configuration cache (modular)`() = withProject {
    modular = true
  }.test {
    runAndAssert("jar", "--configuration-cache") {
      val jar = task(":jar")
      assertEquals(TaskOutcome.SUCCESS, jar?.outcome)
      val mergedJarTask = task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR)
      val optimizedJarTask = task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)
      assertNull(mergedJarTask, "merged jar task should not be in task graph by default")
      assertNull(optimizedJarTask, "optimized jar task should not be in task graph by default")
    }
    runAndAssert("jar", "--configuration-cache") {
      val jar2 = task(":jar")
      assertEquals(TaskOutcome.UP_TO_DATE, jar2?.outcome)
    }
  }

  @Test fun `build non-mrjar with mrjar plugin applied and configuration cache (non-modular)`() =
    withDefaultProject().test {
      runAndAssert("jar", "--configuration-cache") {
        assertEquals(TaskOutcome.SUCCESS, task(":jar")?.outcome)
        val mergedJarTask = task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR)
        val optimizedJarTask = task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)
        assertNull(mergedJarTask, "merged jar task should not be in task graph by default")
        assertNull(optimizedJarTask, "optimized jar task should not be in task graph by default")
      }
      runAndAssert("jar", "--configuration-cache") {
        assertEquals(TaskOutcome.UP_TO_DATE, task(":jar")?.outcome)
      }
    }

  @Test fun `build thin jars with mrjar plugin applied (modular)`() = withProject {
    modular = true
    properties["conventions.jvm.minimum"] = "11"
  }.test {
    runAndAssert("jarThinJvm11") {
      assertNotNull(
        task(":" + BuildConstants.TaskName.COMPILE_JAVA),
        "sanity: java compile task should be present in graph",
      )
      assertEquals(
        TaskOutcome.SUCCESS,
        task(":" + BuildConstants.TaskName.COMPILE_JAVA)?.outcome,
        "sanity: java compile task should be successful",
      )
      assertNotNull(task(":jarThinJvm11"))
      assertEquals(TaskOutcome.SUCCESS, task(":jarThinJvm11")?.outcome)

      outputPath("libs", "hello-world-jvm11.jar") {
        assertJar {
          assertContains(classesFor(JvmTarget.JVM_11).map { it.qualifiedName }, "dev.elide.Sample")
          assertFalse(
            hasClassFor(JvmTarget.JVM_21, "dev.elide.Sample"),
            "jvm11 tareget should not have jvm21",
          )
          assertFalse(
            hasClassFor(JvmTarget.JVM_17, "dev.elide.Sample"),
            "jvm11 tareget should not have jvm17",
          )
          assertFalse(hasClass("some.other.ClassName"))
        }
      }

      outputPath("libs", "hello-world.jar") {
        assertFalse(exists())
      }
      outputPath("libs", "hello-world-jvm17.jar") {
        assertFalse(exists())
      }
      outputPath("libs", "hello-world-jvm21.jar") {
        assertFalse(exists())
      }
      outputPath("libs", "hello-world-multirelease.jar") {
        assertFalse(exists())
      }
      outputPath("libs", "hello-world-multirelease-all.jar") {
        assertFalse(exists())
      }
    }
  }

  @Test fun `build thin jars with mrjar plugin applied (non-modular)`() = withProject {
    modular = false
    properties["conventions.jvm.minimum"] = "11"
  }.test {
    runAndAssert(Java9Modularity.Constants.TaskName.MERGED_MR_JAR) {
      assertNotNull(
        task(":" + BuildConstants.TaskName.COMPILE_JAVA),
        "sanity: java compile task should be present in graph",
      )
      assertEquals(
        TaskOutcome.SUCCESS,
        task(":" + BuildConstants.TaskName.COMPILE_JAVA)?.outcome,
        "sanity: java compile task should be successful",
      )
      val task = assertNotNull(task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR))
      assertEquals(TaskOutcome.SUCCESS, task.outcome)

      // thin libs will have been built to satisfy the merged jar

      outputPath("libs", "hello-world-jvm11.jar") {
        assertJar {
          assertContains(classesFor(JvmTarget.JVM_11).map { it.qualifiedName }, "dev.elide.Sample")
          assertFalse(
            hasClassFor(JvmTarget.JVM_21, "dev.elide.Sample"),
            "jvm11 tareget should not have jvm21",
          )
          assertFalse(
            hasClassFor(JvmTarget.JVM_17, "dev.elide.Sample"),
            "jvm11 tareget should not have jvm17",
          )
          assertFalse(hasClass("some.other.ClassName"))
        }
      }

      outputPath("libs", "hello-world-multirelease-all.jar") {
        assertExists()
        assertReadable()
        assertNonEmpty()
        assertJar {
          assertContains(classesFor(JvmTarget.JVM_11).map { it.qualifiedName }, "dev.elide.Sample")
          assertContains(classesFor(JvmTarget.JVM_17).map { it.qualifiedName }, "dev.elide.Sample")
        }
      }

      // final optimized jar should not be built when only requesting the merged jar

      outputPath("libs", "hello-world-multirelease.jar") {
        assertFalse(exists())
      }
    }
  }

  @Test fun `build merged mrjar with mrjar plugin (modular)`() = withProject {
    modular = true
    properties["conventions.jvm.minimum"] = "11"
  }.test {
    runAndAssert(Java9Modularity.Constants.TaskName.MERGED_MR_JAR) {
      assertNotNull(
        task(":" + BuildConstants.TaskName.COMPILE_JAVA),
        "sanity: java compile task should be present in graph",
      )
      assertEquals(
        TaskOutcome.SUCCESS,
        task(":" + BuildConstants.TaskName.COMPILE_JAVA)?.outcome,
        "sanity: java compile task should be successful",
      )
      val task = assertNotNull(task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR))
      assertEquals(TaskOutcome.SUCCESS, task.outcome)

      // thin libs will have been built to satisfy the merged jar

      outputPath("libs", "hello-world-jvm11.jar") {
        assertJar {
          assertContains(classesFor(JvmTarget.JVM_11).map { it.qualifiedName }, "dev.elide.Sample")
          assertFalse(
            hasClassFor(JvmTarget.JVM_21, "dev.elide.Sample"),
            "jvm11 tareget should not have jvm21",
          )
          assertFalse(
            hasClassFor(JvmTarget.JVM_17, "dev.elide.Sample"),
            "jvm11 tareget should not have jvm17",
          )
          assertFalse(hasClass("some.other.ClassName"))
        }
      }

      outputPath("libs", "hello-world-multirelease-all.jar") {
        assertExists()
        assertReadable()
        assertNonEmpty()
        assertJar {
          assertContains(classesFor(JvmTarget.JVM_11).map { it.qualifiedName }, "dev.elide.Sample")
          assertContains(classesFor(JvmTarget.JVM_17).map { it.qualifiedName }, "dev.elide.Sample")
        }
      }

      // final optimized jar should not be built when only requesting the merged jar

      outputPath("libs", "hello-world-multirelease.jar") {
        assertFalse(exists())
      }
    }
  }

  @Test fun `build merged mrjar with mrjar plugin (non-modular)`() = withProject {
    modular = false
    properties["conventions.jvm.minimum"] = "11"
  }.test {
    runAndAssert(Java9Modularity.Constants.TaskName.MERGED_MR_JAR) {
      assertNotNull(
        task(":" + BuildConstants.TaskName.COMPILE_JAVA),
        "sanity: java compile task should be present in graph",
      )
      assertEquals(
        TaskOutcome.SUCCESS,
        task(":" + BuildConstants.TaskName.COMPILE_JAVA)?.outcome,
        "sanity: java compile task should be successful",
      )
      val task = assertNotNull(task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR))
      assertEquals(TaskOutcome.SUCCESS, task.outcome)

      // thin libs will have been built to satisfy the merged jar

      outputPath("libs", "hello-world-jvm11.jar") {
        assertJar {
          assertContains(classesFor(JvmTarget.JVM_11).map { it.qualifiedName }, "dev.elide.Sample")
          assertFalse(
            hasClassFor(JvmTarget.JVM_21, "dev.elide.Sample"),
            "jvm11 tareget should not have jvm21",
          )
          assertFalse(
            hasClassFor(JvmTarget.JVM_17, "dev.elide.Sample"),
            "jvm11 tareget should not have jvm17",
          )
          assertFalse(hasClass("some.other.ClassName"))
        }
      }

      outputPath("libs", "hello-world-multirelease-all.jar") {
        assertExists()
        assertReadable()
        assertNonEmpty()
        assertJar {
          assertContains(classesFor(JvmTarget.JVM_11).map { it.qualifiedName }, "dev.elide.Sample")
          assertContains(classesFor(JvmTarget.JVM_17).map { it.qualifiedName }, "dev.elide.Sample")
        }
      }

      // final optimized jar should not be built when only requesting the merged jar

      outputPath("libs", "hello-world-multirelease.jar") {
        assertFalse(exists())
      }
    }
  }

  @Test fun `build optimized mrjar in modern-preferred mode (modular)`() = withProject {
    modular = true
    properties["conventions.jvm.minimum"] = "11"
    buildGradleKts = """
        plugins {
          java
          `java-library`
          id("dev.elide.mrjar")
        }
        mrjar {
          preferModern = true
        }
      """.trimIndent()
  }.test {
    runAndAssert("tasks") {
      assertEquals(TaskOutcome.SUCCESS, task(":tasks")?.outcome)
      assertContains(output, Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)

    }
    runAndAssert("build", Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR) {
      assertNotNull(
        task(":" + BuildConstants.TaskName.COMPILE_JAVA),
        "sanity: java compile task should be present in graph",
      )
      assertEquals(
        TaskOutcome.SUCCESS,
        task(":" + BuildConstants.TaskName.COMPILE_JAVA)?.outcome,
        "sanity: java compile task should be successful",
      )
      assertNotNull(
        task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR),
        "merged mrjar task '${Java9Modularity.Constants.TaskName.MERGED_MR_JAR}' should be present",
      )
      assertEquals(
        TaskOutcome.SUCCESS,
        task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR)?.outcome,
        "merged mr jar task should be successful",
      )
      assertNotNull(
        task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR),
        "optimized mrjar task '${Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR}' should be present",
      )
      assertEquals(
        TaskOutcome.SUCCESS,
        task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)?.outcome,
        "optimized mr jar task should be successful",
      )
      assertEquals(TaskOutcome.SUCCESS, task(":build")?.outcome)

      // Optimized MRJAR should have all target classes and the module info

      outputPath("libs", "hello-world-multirelease.jar") {
        assertJar {
          assertTrue(hasClass("module-info"))
          assertContains(classesFor(JvmTarget.JVM_21).map { it.qualifiedName }, "dev.elide.Sample")
          assertContains(classesFor(JvmTarget.JVM_17).map { it.qualifiedName }, "dev.elide.Sample")
          assertContains(classesFor(JvmTarget.JVM_11).map { it.qualifiedName }, "dev.elide.Sample")
          assertTrue("Multi-Release" in manifest())
          assertTrue(isModular())
          assertTrue(isMultiRelease())
          assertFalse(hasClass("some.other.ClassName"))
        }
      }
    }
  }

  @Test fun `build optimized mrjar in modern-preferred mode (non-modular)`() = withProject {
    modular = false
    properties["conventions.jvm.minimum"] = "11"
    buildGradleKts = """
        plugins {
          java
          `java-library`
          id("dev.elide.mrjar")
        }
        mrjar {
          preferModern = true
        }
      """.trimIndent()
  }.test {
    runAndAssert("tasks") {
      assertEquals(TaskOutcome.SUCCESS, task(":tasks")?.outcome)
      assertContains(output, Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)
    }
    runAndAssert("build", Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR) {
      assertNotNull(
        task(":" + BuildConstants.TaskName.COMPILE_JAVA),
        "sanity: java compile task should be present in graph",
      )
      assertEquals(
        TaskOutcome.SUCCESS,
        task(":" + BuildConstants.TaskName.COMPILE_JAVA)?.outcome,
        "sanity: java compile task should be successful",
      )
      assertNotNull(
        task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR),
        "merged mrjar task '${Java9Modularity.Constants.TaskName.MERGED_MR_JAR}' should be present",
      )
      assertEquals(
        TaskOutcome.SUCCESS,
        task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR)?.outcome,
        "merged mr jar task should be successful",
      )
      assertNotNull(
        task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR),
        "optimized mrjar task '${Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR}' should be present",
      )
      assertEquals(
        TaskOutcome.SUCCESS,
        task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)?.outcome,
        "optimized mr jar task should be successful",
      )
      assertEquals(TaskOutcome.SUCCESS, task(":build")?.outcome)

      // Optimized MRJAR should have all target classes and the module info

      outputPath("libs", "hello-world-multirelease.jar") {
        assertJar {
          assertFalse(hasClass("module-info"))
          assertContains(classesFor(JvmTarget.JVM_21).map { it.qualifiedName }, "dev.elide.Sample")
          assertContains(classesFor(JvmTarget.JVM_17).map { it.qualifiedName }, "dev.elide.Sample")
          assertContains(classesFor(JvmTarget.JVM_11).map { it.qualifiedName }, "dev.elide.Sample")
          assertTrue("Multi-Release" in manifest())
          assertFalse(isModular())
          assertTrue(isMultiRelease())
          assertFalse(hasClass("some.other.ClassName"))
        }
      }
    }
  }

  // @TODO: allReleases is not implemented yet
  @Test @Ignore fun `build optimized mrjar with all releases (modular)`() = withProject {
    modular = true
    properties["conventions.jvm.minimum"] = "11"
    buildGradleKts = """
        plugins {
          java
          `java-library`
          id("dev.elide.mrjar")
        }
        mrjar {
          preferModern = false
          allReleases = true
        }
      """.trimIndent()
  }.test {
    runAndAssert("tasks") {
      assertEquals(TaskOutcome.SUCCESS, task(":tasks")?.outcome)
      assertContains(output, Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)
    }
    runAndAssert("build", Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR) {
      assertNotNull(
        task(":" + BuildConstants.TaskName.COMPILE_JAVA),
        "sanity: java compile task should be present in graph",
      )
      assertEquals(
        TaskOutcome.SUCCESS,
        task(":" + BuildConstants.TaskName.COMPILE_JAVA)?.outcome,
        "sanity: java compile task should be successful",
      )
      assertNotNull(
        task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR),
        "merged mrjar task '${Java9Modularity.Constants.TaskName.MERGED_MR_JAR}' should be present",
      )
      assertEquals(
        TaskOutcome.SUCCESS,
        task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR)?.outcome,
        "merged mr jar task should be successful",
      )
      assertNotNull(
        task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR),
        "optimized mrjar task '${Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR}' should be present",
      )
      assertEquals(
        TaskOutcome.SUCCESS,
        task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)?.outcome,
        "optimized mr jar task should be successful",
      )
      assertEquals(TaskOutcome.SUCCESS, task(":build")?.outcome)

      // Optimized MRJAR should have all target classes and the module info

      outputPath("libs", "hello-world-multirelease.jar") {
        assertJar {
          // does not cross JDK8 boundary
          assertTrue(hasClass("module-info"))

          // we told it to produce all releases
          listOf(
            JvmTarget.JVM_21,
            JvmTarget.JVM_20,
            JvmTarget.JVM_19,
            JvmTarget.JVM_18,
            JvmTarget.JVM_17,
            JvmTarget.JVM_16,
            JvmTarget.JVM_15,
            JvmTarget.JVM_14,
            JvmTarget.JVM_13,
            JvmTarget.JVM_12,
            JvmTarget.JVM_11,
          ).forEach {
            assertContains(
              classesFor(it, strict = true).map { cls -> cls.qualifiedName },
              "dev.elide.Sample",
              StringBuilder().apply {
                append("Missing class 'dev.elide.Sample' for JVM target $it. Present for: ")

                classes().filter { cls ->
                  cls.qualifiedName == "dev.elide.Sample"
                }.joinToString(", ") { cls ->
                  when (val target = cls.target) {
                    null -> "unspecified target"
                    else -> "JVM ${target.target}"
                  }
                }.also { targetList ->
                  append(targetList)
                }
              }.toString(),
            )
          }

          // is a modular multi-release from root
          assertTrue("Multi-Release" in manifest())
          assertTrue(isModular())
          assertTrue(isMultiRelease())
          assertFalse(hasClass("some.other.ClassName"))
        }
      }
    }
  }

  // @TODO: allReleases is not implemented yet
  @Test @Ignore fun `build optimized mrjar with all releases in modern-preferred mode (modular)`() {

  }

  // @TODO: allReleases is not implemented yet
  @Test @Ignore fun `build optimized mrjar with all releases (non-modular)`() = withProject {
    modular = false
    properties["conventions.jvm.minimum"] = "11"
    buildGradleKts = """
        plugins {
          java
          `java-library`
          id("dev.elide.mrjar")
        }
        mrjar {
          preferModern = false
          allReleases = true
        }
      """.trimIndent()
  }.test {
    runAndAssert("tasks") {
      assertEquals(TaskOutcome.SUCCESS, task(":tasks")?.outcome)
      assertContains(output, Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)
    }
    runAndAssert("build", Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR) {
      assertNotNull(
        task(":" + BuildConstants.TaskName.COMPILE_JAVA),
        "sanity: java compile task should be present in graph",
      )
      assertEquals(
        TaskOutcome.SUCCESS,
        task(":" + BuildConstants.TaskName.COMPILE_JAVA)?.outcome,
        "sanity: java compile task should be successful",
      )
      assertNotNull(
        task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR),
        "merged mrjar task '${Java9Modularity.Constants.TaskName.MERGED_MR_JAR}' should be present",
      )
      assertEquals(
        TaskOutcome.SUCCESS,
        task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR)?.outcome,
        "merged mr jar task should be successful",
      )
      assertNotNull(
        task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR),
        "optimized mrjar task '${Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR}' should be present",
      )
      assertEquals(
        TaskOutcome.SUCCESS,
        task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)?.outcome,
        "optimized mr jar task should be successful",
      )
      assertEquals(TaskOutcome.SUCCESS, task(":build")?.outcome)

      // Optimized MRJAR should have all target classes and the module info

      outputPath("libs", "hello-world-multirelease.jar") {
        assertJar {
          // is not a module at all
          assertFalse(hasClass("module-info"))

          // we told it to produce all releases
          listOf(
            JvmTarget.JVM_21,
            JvmTarget.JVM_20,
            JvmTarget.JVM_19,
            JvmTarget.JVM_18,
            JvmTarget.JVM_17,
            JvmTarget.JVM_16,
            JvmTarget.JVM_15,
            JvmTarget.JVM_14,
            JvmTarget.JVM_13,
            JvmTarget.JVM_12,
            JvmTarget.JVM_11,
          ).forEach {
            assertContains(
              classesFor(it, strict = true).map { cls -> cls.qualifiedName },
              "dev.elide.Sample",
              StringBuilder().apply {
                append("Missing class 'dev.elide.Sample' for JVM target $it. Present for: ")

                classes().filter { cls ->
                  cls.qualifiedName == "dev.elide.Sample"
                }.joinToString(", ") { cls ->
                  when (val target = cls.target) {
                    null -> "unspecified target"
                    else -> "JVM ${target.target}"
                  }
                }.also { targetList ->
                  append(targetList)
                }
              }.toString(),
            )
          }

          // is a modular multi-release from root
          assertTrue("Multi-Release" in manifest())
          assertFalse(isModular())
          assertTrue(isMultiRelease())
          assertFalse(hasClass("some.other.ClassName"))
        }
      }
    }
  }

  // @TODO: allReleases is not implemented yet
  @Test @Ignore fun `build optimized mrjar with all releases in modern-preferred mode (non-modular)`() {

  }
}
