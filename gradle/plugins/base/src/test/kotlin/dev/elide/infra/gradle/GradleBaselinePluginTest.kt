package dev.elide.infra.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GradleBaselinePluginTest : AbstractPluginTest() {
  @Test fun `apply baseline plugin and query tasks`() {
    writeFile(settingsFile, "rootProject.name = \"hello-world\"")
    val buildFileContent = "plugins {\n" +
      "    id(\"dev.elide.base\")\n" +
      "}\n"
    writeFile(buildFile, buildFileContent)

    val result: BuildResult = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withArguments("tasks")
      .withPluginClasspath()
      .build()

    assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
  }

  @Test fun `apply baseline plugin in combination with java plugin`() {
    writeFile(settingsFile, "rootProject.name = \"hello-world\"")
    val buildFileContent = StringBuilder().apply {
      gradleKts("""
        plugins {
          java
          `java-library`
          id("dev.elide.base")
        }

        java {
          targetCompatibility = JavaVersion.VERSION_21
          sourceCompatibility = JavaVersion.VERSION_21
        }
      """)
    }

    writeFile(buildFile, buildFileContent.toString())

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
  }

  @Test fun `baseline plugin should make archives reproducible`() {
    writeFile(settingsFile, "rootProject.name = \"hello-world\"")
    val buildFileContent = StringBuilder().apply {
      gradleKts("""
        plugins {
          java
          `java-library`
          id("dev.elide.base")
        }

        java {
          targetCompatibility = JavaVersion.VERSION_21
          sourceCompatibility = JavaVersion.VERSION_21
        }
      """)
    }

    writeFile(buildFile, buildFileContent.toString())

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
  }
}
