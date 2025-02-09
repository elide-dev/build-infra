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

package dev.elide.infra.gradle.jmod

import dev.elide.infra.gradle.AbstractPluginTest
import dev.elide.infra.gradle.TestSourceFileLanguage
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertNull

class GradleJModPluginTest : AbstractPluginTest() {
  @Test fun `apply jmod plugin and query tasks`() {
    writeFile(settingsFile, "rootProject.name = \"hello-world\"")
    val buildFileContent = "plugins {\n" +
      "    java\n" +
      "    `java-library`\n" +
      "    id(\"dev.elide.jmod\")\n" +
      "}\n"
    writeFile(buildFile, buildFileContent)

    val result: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments("tasks")
      .withPluginClasspath()
      .build()

    assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
  }

  @Test fun `apply jmod plugin and disable it`() {
    writeFile(settingsFile, "rootProject.name = \"hello-world\"")
    val buildFileContent = "plugins {\n" +
      "    java\n" +
      "    `java-library`\n" +
      "    id(\"dev.elide.jmod\")\n" +
      "}\n" +
      "\n" +
      "jmod {\n" +
      "    enabled = false\n" +
      "}\n"
    writeFile(buildFile, buildFileContent)

    val result: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments("tasks")
      .withPluginClasspath()
      .build()

    assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
  }

  @Test fun `jmod task name should be listed in tasks output`() {
    writeFile(settingsFile, "rootProject.name = \"hello-world\"")
    val buildFileContent = "plugins {\n" +
      "    java\n" +
      "    `java-library`\n" +
      "    id(\"dev.elide.jmod\")\n" +
      "}\n"
    writeFile(buildFile, buildFileContent)

    val result: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments("tasks")
      .withPluginClasspath()
      .build()

    assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    assertTrue("jmod" in result.output)
  }

  @Test fun `build simple jmod artifact with jmod plugin`() {
    writeFile(settingsFile, "rootProject.name = \"hello-world\"")
    val buildFileContent = StringBuilder().apply {
      gradleKts("""
        plugins {
          java
          `java-library`
          id("dev.elide.jmod")
        }

        java {
          targetCompatibility = JavaVersion.VERSION_21
          sourceCompatibility = JavaVersion.VERSION_21
        }
      """)
    }

    writeFile(buildFile, buildFileContent.toString())

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
      .withArguments("build", "jmod", "jar")
      .withPluginClasspath()
      .build()

    val jar = result.task(":jar")
    assertEquals(TaskOutcome.SUCCESS, jar?.outcome)
    val jmod = result.task(":jmod")
    assertEquals(TaskOutcome.SUCCESS, jmod?.outcome)

    val target = outputPath("jmod/hello-world.jmod").toFile()
    assertTrue(target.exists(), "output jmod should exist at '${target.path}'")
  }

  @Test fun `regular build should not trigger jmod task`() {
    writeFile(settingsFile, "rootProject.name = \"hello-world\"")
    val buildFileContent = StringBuilder().apply {
      gradleKts("""
        plugins {
          java
          `java-library`
          id("dev.elide.jmod")
        }

        java {
          targetCompatibility = JavaVersion.VERSION_21
          sourceCompatibility = JavaVersion.VERSION_21
        }
      """)
    }

    writeFile(buildFile, buildFileContent.toString())

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
      .withArguments("build")
      .withPluginClasspath()
      .build()

    val jar = result.task(":jar")
    assertEquals(TaskOutcome.SUCCESS, jar?.outcome)
    assertNull(result.task(":jmod"))
  }
}
