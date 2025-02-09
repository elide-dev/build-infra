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

class GradleMultiReleasePluginTest : AbstractPluginTest() {
  @Test fun `apply mrjar plugin and query tasks`() {
    writeFile(buildFile) {
      gradleKts("""
        plugins {
          java
          `java-library`
          id("dev.elide.mrjar")
        }
      """)
    }

    val result: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments("tasks", "--stacktrace")
      .withPluginClasspath()
      .build()

    assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
  }

  @Test fun `optimized mrjar should appear as task option (non-modular)`() {
    writeFile(buildFile) {
      gradleKts("""
        plugins {
          java
          `java-library`
          id("dev.elide.mrjar")
        }
      """)
    }

    writeFile(gradleProperties) {
      properties("""
        # build conventions: jvm
        conventions.jvm.minimum=11
      """)
    }

    val result: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments("tasks")
      .withPluginClasspath()
      .build()

    assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    assertContains(result.output, Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)
  }

  @Test fun `optimized mrjar should appear as task option (modular)`() {
    writeFile(buildFile) {
      gradleKts("""
        plugins {
          java
          `java-library`
          id("dev.elide.mrjar")
        }
      """)
    }

    writeFile(gradleProperties) {
      properties("""
        # build conventions: jvm
        conventions.jvm.minimum=11
      """)
    }

    writeSourceFile(TestSourceFileLanguage.JAVA, "module-info.java") {
      java("""
        module sample.test {
          requires java.base;
        }
      """)
    }

    val result: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments("tasks")
      .withPluginClasspath()
      .build()

    assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    assertTrue(result.output.contains(Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR))
  }

  @Test fun `build modular mrjar with jvm minimum target via properties`() {
    writeFile(buildFile) {
      gradleKts("""
        plugins {
          java
          `java-library`
          id("dev.elide.mrjar")
        }
      """)
    }

    writeFile(gradleProperties) {
      properties("""
        # build conventions: jvm
        conventions.jvm.minimum=11
      """)
    }

    writeSourceFile(TestSourceFileLanguage.JAVA, "module-info.java") {
      java("""
        module sample.test {
          requires java.base;
        }
      """)
    }

    writeSourceFile(TestSourceFileLanguage.JAVA, "dev/elide/Sample.java") {
      java("""
        package dev.elide;

        public final class Sample {
          public final static String sample = "SAMPLE";
        }
      """)
    }

    val result: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments("build")
      .withPluginClasspath()
      .build()

    assertNotNull(
      result.task(":" + BuildConstants.TaskName.COMPILE_JAVA),
      "sanity: java compile task should be present in graph",
    )
    assertEquals(
      TaskOutcome.SUCCESS,
      result.task(":" + BuildConstants.TaskName.COMPILE_JAVA)?.outcome,
      "sanity: java compile task should be successful",
    )
    assertNotNull(
      result.task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR),
      "merged mrjar task '${Java9Modularity.Constants.TaskName.MERGED_MR_JAR}' should be present",
    )
    assertEquals(
      TaskOutcome.SUCCESS,
      result.task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR)?.outcome,
      "merged mr jar task should be successful",
    )
    assertNotNull(
      result.task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR),
      "optimized mrjar task '${Java9Modularity.Constants.TaskName.MERGED_MR_JAR}' should be present",
    )
    assertEquals(
      TaskOutcome.SUCCESS,
      result.task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)?.outcome,
      "optimized mr jar task should be successful",
    )
    assertEquals(TaskOutcome.SUCCESS, result.task(":build")?.outcome)

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

  @Test fun `build non-mrjar with mrjar plugin applied (modular)`() {
    writeFile(buildFile) {
      gradleKts("""
        plugins {
          java
          `java-library`
          id("dev.elide.mrjar")
        }
      """)
    }

    writeSourceFile(TestSourceFileLanguage.JAVA, "module-info.java") {
      java("""
        module testmod {
          requires java.base;
        }
      """)
    }

    writeSourceFile(TestSourceFileLanguage.JAVA, "dev/test/Sample.java") {
      java("""
        package dev.test;

        public final class Sample {
          public final static String Sample = "SAMPLE";
        }
      """)
    }

    val result: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments("jar")
      .withPluginClasspath()
      .build()

    val jar = result.task(":jar")
    assertEquals(TaskOutcome.SUCCESS, jar?.outcome)
    val mergedJarTask = result.task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR)
    val optimizedJarTask = result.task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)
    assertNull(mergedJarTask, "merged jar task should not be in task graph by default")
    assertNull(optimizedJarTask, "optimized jar task should not be in task graph by default")
  }

  @Test fun `build non-mrjar with mrjar plugin applied (non-modular)`() {
    writeFile(buildFile) {
      gradleKts("""
        plugins {
          java
          `java-library`
          id("dev.elide.mrjar")
        }
      """)
    }

    writeSourceFile(TestSourceFileLanguage.JAVA, "dev/test/Sample.java") {
      java("""
        package dev.test;

        public final class Sample {
          public final static String Sample = "SAMPLE";
        }
      """)
    }

    val result: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments("jar")
      .withPluginClasspath()
      .build()

    val jar = result.task(":jar")
    assertEquals(TaskOutcome.SUCCESS, jar?.outcome)
    val mergedJarTask = result.task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR)
    val optimizedJarTask = result.task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)
    assertNull(mergedJarTask, "merged jar task should not be in task graph by default")
    assertNull(optimizedJarTask, "optimized jar task should not be in task graph by default")
  }

  @Test fun `build non-mrjar with mrjar plugin applied and configuration cache (modular)`() {
    writeFile(buildFile) {
      gradleKts("""
        plugins {
          java
          `java-library`
          id("dev.elide.mrjar")
        }
      """)
    }

    writeSourceFile(TestSourceFileLanguage.JAVA, "module-info.java") {
      java("""
        module testmod {
          requires java.base;
        }
      """)
    }

    writeSourceFile(TestSourceFileLanguage.JAVA, "dev/test/Sample.java") {
      java("""
        package dev.test;

        public final class Sample {
          public final static String Sample = "SAMPLE";
        }
      """)
    }

    val result: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments("jar", "--configuration-cache")
      .withPluginClasspath()
      .build()

    val jar = result.task(":jar")
    assertEquals(TaskOutcome.SUCCESS, jar?.outcome)
    val mergedJarTask = result.task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR)
    val optimizedJarTask = result.task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)
    assertNull(mergedJarTask, "merged jar task should not be in task graph by default")
    assertNull(optimizedJarTask, "optimized jar task should not be in task graph by default")

    val result2: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments("jar", "--configuration-cache")
      .withPluginClasspath()
      .build()

    val jar2 = result2.task(":jar")
    assertEquals(TaskOutcome.UP_TO_DATE, jar2?.outcome)
  }

  @Test fun `build non-mrjar with mrjar plugin applied and configuration cache (non-modular)`() {
    writeFile(buildFile) {
      gradleKts("""
        plugins {
          java
          `java-library`
          id("dev.elide.mrjar")
        }
      """)
    }

    writeSourceFile(TestSourceFileLanguage.JAVA, "dev/test/Sample.java") {
      java("""
        package dev.test;

        public final class Sample {
          public final static String Sample = "SAMPLE";
        }
      """)
    }

    val result: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments("jar", "--configuration-cache")
      .withPluginClasspath()
      .build()

    val jar = result.task(":jar")
    assertEquals(TaskOutcome.SUCCESS, jar?.outcome)
    val mergedJarTask = result.task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR)
    val optimizedJarTask = result.task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)
    assertNull(mergedJarTask, "merged jar task should not be in task graph by default")
    assertNull(optimizedJarTask, "optimized jar task should not be in task graph by default")

    val result2: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments("jar", "--configuration-cache")
      .withPluginClasspath()
      .build()

    val jar2 = result2.task(":jar")
    assertEquals(TaskOutcome.UP_TO_DATE, jar2?.outcome)
  }

  @Test fun `build thin jars with mrjar plugin applied (modular)`() {
    writeFile(buildFile) {
      gradleKts("""
        plugins {
          java
          `java-library`
          id("dev.elide.mrjar")
        }
      """)
    }

    writeFile(gradleProperties) {
      properties("""
        # build conventions: jvm
        conventions.jvm.minimum=11
      """)
    }

    writeSourceFile(TestSourceFileLanguage.JAVA, "module-info.java") {
      java("""
        module sample.test {
          requires java.base;
        }
      """)
    }

    writeSourceFile(TestSourceFileLanguage.JAVA, "dev/elide/Sample.java") {
      java("""
        package dev.elide;

        public final class Sample {
          public final static String sample = "SAMPLE";
        }
      """)
    }

    val result: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments("jarThinJvm11")
      .withPluginClasspath()
      .build()

    assertNotNull(
      result.task(":" + BuildConstants.TaskName.COMPILE_JAVA),
      "sanity: java compile task should be present in graph",
    )
    assertEquals(
      TaskOutcome.SUCCESS,
      result.task(":" + BuildConstants.TaskName.COMPILE_JAVA)?.outcome,
      "sanity: java compile task should be successful",
    )
    assertNotNull(result.task(":jarThinJvm11"))
    assertEquals(TaskOutcome.SUCCESS, result.task(":jarThinJvm11")?.outcome)

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

  @Test fun `build thin jars with mrjar plugin applied (non-modular)`() {
    writeFile(buildFile) {
      gradleKts("""
        plugins {
          java
          `java-library`
          id("dev.elide.mrjar")
        }
      """)
    }

    writeFile(gradleProperties) {
      properties("""
        # build conventions: jvm
        conventions.jvm.minimum=11
      """)
    }

    writeSourceFile(TestSourceFileLanguage.JAVA, "dev/elide/Sample.java") {
      java("""
        package dev.elide;

        public final class Sample {
          public final static String sample = "SAMPLE";
        }
      """)
    }

    val result: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments(Java9Modularity.Constants.TaskName.MERGED_MR_JAR)
      .withPluginClasspath()
      .build()

    assertNotNull(
      result.task(":" + BuildConstants.TaskName.COMPILE_JAVA),
      "sanity: java compile task should be present in graph",
    )
    assertEquals(
      TaskOutcome.SUCCESS,
      result.task(":" + BuildConstants.TaskName.COMPILE_JAVA)?.outcome,
      "sanity: java compile task should be successful",
    )
    val task = assertNotNull(result.task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR))
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

  @Test fun `build merged mrjar with mrjar plugin (modular)`() {
    writeFile(buildFile) {
      gradleKts("""
        plugins {
          java
          `java-library`
          id("dev.elide.mrjar")
        }
      """)
    }

    writeFile(gradleProperties) {
      properties("""
        # build conventions: jvm
        conventions.jvm.minimum=11
      """)
    }

    writeSourceFile(TestSourceFileLanguage.JAVA, "module-info.java") {
      java("""
        module sample.test {
          requires java.base;
        }
      """)
    }

    writeSourceFile(TestSourceFileLanguage.JAVA, "dev/elide/Sample.java") {
      java("""
        package dev.elide;

        public final class Sample {
          public final static String sample = "SAMPLE";
        }
      """)
    }

    val result: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments(Java9Modularity.Constants.TaskName.MERGED_MR_JAR)
      .withPluginClasspath()
      .build()

    assertNotNull(
      result.task(":" + BuildConstants.TaskName.COMPILE_JAVA),
      "sanity: java compile task should be present in graph",
    )
    assertEquals(
      TaskOutcome.SUCCESS,
      result.task(":" + BuildConstants.TaskName.COMPILE_JAVA)?.outcome,
      "sanity: java compile task should be successful",
    )
    val task = assertNotNull(result.task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR))
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

  @Test fun `build merged mrjar with mrjar plugin (non-modular)`() {
    writeFile(buildFile) {
      gradleKts("""
        plugins {
          java
          `java-library`
          id("dev.elide.mrjar")
        }
      """)
    }

    writeFile(gradleProperties) {
      properties("""
        # build conventions: jvm
        conventions.jvm.minimum=11
      """)
    }

    writeSourceFile(TestSourceFileLanguage.JAVA, "dev/elide/Sample.java") {
      java("""
        package dev.elide;

        public final class Sample {
          public final static String sample = "SAMPLE";
        }
      """)
    }

    val result: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments(Java9Modularity.Constants.TaskName.MERGED_MR_JAR)
      .withPluginClasspath()
      .build()

    assertNotNull(
      result.task(":" + BuildConstants.TaskName.COMPILE_JAVA),
      "sanity: java compile task should be present in graph",
    )
    assertEquals(
      TaskOutcome.SUCCESS,
      result.task(":" + BuildConstants.TaskName.COMPILE_JAVA)?.outcome,
      "sanity: java compile task should be successful",
    )
    val task = assertNotNull(result.task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR))
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

  @Test fun `build optimized mrjar in modern-preferred mode (modular)`() {
    writeFile(buildFile) {
      gradleKts("""
        plugins {
          java
          `java-library`
          id("dev.elide.mrjar")
        }
        mrjar {
          preferModern = true
        }
      """)
    }

    writeFile(gradleProperties) {
      properties("""
        # build conventions: jvm
        conventions.jvm.minimum=11
      """)
    }

    writeSourceFile(TestSourceFileLanguage.JAVA, "module-info.java") {
      java("""
        module sample.test {
          requires java.base;
        }
      """)
    }

    writeSourceFile(TestSourceFileLanguage.JAVA, "dev/elide/Sample.java") {
      java("""
        package dev.elide;

        public final class Sample {
          public final static String sample = "SAMPLE";
        }
      """)
    }

    val tasksResult: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments("tasks")
      .withPluginClasspath()
      .build()

    assertEquals(TaskOutcome.SUCCESS, tasksResult.task(":tasks")?.outcome)
    assertContains(tasksResult.output, Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)

    val result: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments("build", Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)
      .withPluginClasspath()
      .build()

    assertNotNull(
      result.task(":" + BuildConstants.TaskName.COMPILE_JAVA),
      "sanity: java compile task should be present in graph",
    )
    assertEquals(
      TaskOutcome.SUCCESS,
      result.task(":" + BuildConstants.TaskName.COMPILE_JAVA)?.outcome,
      "sanity: java compile task should be successful",
    )
    assertNotNull(
      result.task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR),
      "merged mrjar task '${Java9Modularity.Constants.TaskName.MERGED_MR_JAR}' should be present",
    )
    assertEquals(
      TaskOutcome.SUCCESS,
      result.task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR)?.outcome,
      "merged mr jar task should be successful",
    )
    assertNotNull(
      result.task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR),
      "optimized mrjar task '${Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR}' should be present",
    )
    assertEquals(
      TaskOutcome.SUCCESS,
      result.task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)?.outcome,
      "optimized mr jar task should be successful",
    )
    assertEquals(TaskOutcome.SUCCESS, result.task(":build")?.outcome)

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

  @Test fun `build optimized mrjar in modern-preferred mode (non-modular)`() {
    writeFile(buildFile) {
      gradleKts("""
        plugins {
          java
          `java-library`
          id("dev.elide.mrjar")
        }
        mrjar {
          preferModern = true
        }
      """)
    }

    writeFile(gradleProperties) {
      properties("""
        # build conventions: jvm
        conventions.jvm.minimum=11
      """)
    }

    writeSourceFile(TestSourceFileLanguage.JAVA, "dev/elide/Sample.java") {
      java("""
        package dev.elide;

        public final class Sample {
          public final static String sample = "SAMPLE";
        }
      """)
    }

    val tasksResult: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments("tasks")
      .withPluginClasspath()
      .build()

    assertEquals(TaskOutcome.SUCCESS, tasksResult.task(":tasks")?.outcome)
    assertContains(tasksResult.output, Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)

    val result: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments("build", Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)
      .withPluginClasspath()
      .build()

    assertNotNull(
      result.task(":" + BuildConstants.TaskName.COMPILE_JAVA),
      "sanity: java compile task should be present in graph",
    )
    assertEquals(
      TaskOutcome.SUCCESS,
      result.task(":" + BuildConstants.TaskName.COMPILE_JAVA)?.outcome,
      "sanity: java compile task should be successful",
    )
    assertNotNull(
      result.task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR),
      "merged mrjar task '${Java9Modularity.Constants.TaskName.MERGED_MR_JAR}' should be present",
    )
    assertEquals(
      TaskOutcome.SUCCESS,
      result.task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR)?.outcome,
      "merged mr jar task should be successful",
    )
    assertNotNull(
      result.task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR),
      "optimized mrjar task '${Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR}' should be present",
    )
    assertEquals(
      TaskOutcome.SUCCESS,
      result.task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)?.outcome,
      "optimized mr jar task should be successful",
    )
    assertEquals(TaskOutcome.SUCCESS, result.task(":build")?.outcome)

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

  // @TODO: allReleases is not implemented yet
  @Test @Ignore fun `build optimized mrjar with all releases (modular)`() {
    writeFile(buildFile) {
      gradleKts("""
        plugins {
          java
          `java-library`
          id("dev.elide.mrjar")
        }
        mrjar {
          preferModern = false
          allReleases = true
        }
      """)
    }

    writeFile(gradleProperties) {
      properties("""
        # build conventions: jvm
        conventions.jvm.minimum=11
      """)
    }

    writeSourceFile(TestSourceFileLanguage.JAVA, "module-info.java") {
      java("""
        module sample.test {
          requires java.base;
        }
      """)
    }

    writeSourceFile(TestSourceFileLanguage.JAVA, "dev/elide/Sample.java") {
      java("""
        package dev.elide;

        public final class Sample {
          public final static String sample = "SAMPLE";
        }
      """)
    }

    val tasksResult: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments("tasks")
      .withPluginClasspath()
      .build()

    assertEquals(TaskOutcome.SUCCESS, tasksResult.task(":tasks")?.outcome)
    assertContains(tasksResult.output, Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)

    val result: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments("build", Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)
      .withPluginClasspath()
      .build()

    assertNotNull(
      result.task(":" + BuildConstants.TaskName.COMPILE_JAVA),
      "sanity: java compile task should be present in graph",
    )
    assertEquals(
      TaskOutcome.SUCCESS,
      result.task(":" + BuildConstants.TaskName.COMPILE_JAVA)?.outcome,
      "sanity: java compile task should be successful",
    )
    assertNotNull(
      result.task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR),
      "merged mrjar task '${Java9Modularity.Constants.TaskName.MERGED_MR_JAR}' should be present",
    )
    assertEquals(
      TaskOutcome.SUCCESS,
      result.task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR)?.outcome,
      "merged mr jar task should be successful",
    )
    assertNotNull(
      result.task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR),
      "optimized mrjar task '${Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR}' should be present",
    )
    assertEquals(
      TaskOutcome.SUCCESS,
      result.task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)?.outcome,
      "optimized mr jar task should be successful",
    )
    assertEquals(TaskOutcome.SUCCESS, result.task(":build")?.outcome)

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

  // @TODO: allReleases is not implemented yet
  @Test @Ignore fun `build optimized mrjar with all releases in modern-preferred mode (modular)`() {

  }

  // @TODO: allReleases is not implemented yet
  @Test @Ignore fun `build optimized mrjar with all releases (non-modular)`() {
    writeFile(buildFile) {
      gradleKts("""
        plugins {
          java
          `java-library`
          id("dev.elide.mrjar")
        }
        mrjar {
          preferModern = false
          allReleases = true
        }
      """)
    }

    writeFile(gradleProperties) {
      properties("""
        # build conventions: jvm
        conventions.jvm.minimum=11
      """)
    }

    writeSourceFile(TestSourceFileLanguage.JAVA, "dev/elide/Sample.java") {
      java("""
        package dev.elide;

        public final class Sample {
          public final static String sample = "SAMPLE";
        }
      """)
    }

    val tasksResult: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments("tasks")
      .withPluginClasspath()
      .build()

    assertEquals(TaskOutcome.SUCCESS, tasksResult.task(":tasks")?.outcome)
    assertContains(tasksResult.output, Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)

    val result: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments("build", Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)
      .withPluginClasspath()
      .build()

    assertNotNull(
      result.task(":" + BuildConstants.TaskName.COMPILE_JAVA),
      "sanity: java compile task should be present in graph",
    )
    assertEquals(
      TaskOutcome.SUCCESS,
      result.task(":" + BuildConstants.TaskName.COMPILE_JAVA)?.outcome,
      "sanity: java compile task should be successful",
    )
    assertNotNull(
      result.task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR),
      "merged mrjar task '${Java9Modularity.Constants.TaskName.MERGED_MR_JAR}' should be present",
    )
    assertEquals(
      TaskOutcome.SUCCESS,
      result.task(":" + Java9Modularity.Constants.TaskName.MERGED_MR_JAR)?.outcome,
      "merged mr jar task should be successful",
    )
    assertNotNull(
      result.task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR),
      "optimized mrjar task '${Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR}' should be present",
    )
    assertEquals(
      TaskOutcome.SUCCESS,
      result.task(":" + Java9Modularity.Constants.TaskName.OPTIMIZED_MR_JAR)?.outcome,
      "optimized mr jar task should be successful",
    )
    assertEquals(TaskOutcome.SUCCESS, result.task(":build")?.outcome)

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

  // @TODO: allReleases is not implemented yet
  @Test @Ignore fun `build optimized mrjar with all releases in modern-preferred mode (non-modular)`() {

  }
}
