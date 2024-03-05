package dev.elide.infra.gradle

import dev.elide.infra.gradle.api.toJavaLanguageVersion
import dev.elide.infra.gradle.api.toJavaVersion
import dev.elide.infra.gradle.api.until
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import kotlin.test.*

class BuildApiTests {
  @Test fun `should be able to translate a JvmTarget to a JavaLanguageVersion`() {
    val target = JvmTarget.JVM_21
    val lang = assertNotNull(target.toJavaLanguageVersion())
    assertEquals(21, lang.asInt())
  }

  @Test fun `should be able to translate a JvmTarget to a JavaVersion`() {
    val target = JvmTarget.JVM_21
    val lang = assertNotNull(target.toJavaVersion())
    assertEquals("21", lang.majorVersion)
  }

  @Test fun `JvmTarget should expose expected target int`() {
    (9..21).forEach {
      assertNotNull(JvmTarget.fromTarget(it.toString()))
      assertEquals(it.toString(), JvmTarget.fromTarget(it.toString()).target)
    }
  }

  @Test fun `should be able generate a range of JvmTarget instances`() {
    val range = assertNotNull(JvmTarget.JVM_9 until JvmTarget.JVM_21)
    assertTrue(JvmTarget.JVM_9 in range)
    assertTrue(JvmTarget.JVM_10 in range)
    assertTrue(JvmTarget.JVM_11 in range)
    assertTrue(JvmTarget.JVM_12 in range)
    assertTrue(JvmTarget.JVM_13 in range)
    assertTrue(JvmTarget.JVM_14 in range)
    assertTrue(JvmTarget.JVM_15 in range)
    assertTrue(JvmTarget.JVM_16 in range)
    assertTrue(JvmTarget.JVM_17 in range)
    assertTrue(JvmTarget.JVM_18 in range)
    assertTrue(JvmTarget.JVM_19 in range)
    assertTrue(JvmTarget.JVM_20 in range)
    assertTrue(JvmTarget.JVM_21 in range)
    assertTrue(JvmTarget.JVM_1_8 !in range)
  }

  @Test fun `target range should only include specified values`() {
    val range = assertNotNull(JvmTarget.JVM_9 until JvmTarget.JVM_17)
    assertTrue(JvmTarget.JVM_9 in range)
    assertTrue(JvmTarget.JVM_10 in range)
    assertTrue(JvmTarget.JVM_11 in range)
    assertTrue(JvmTarget.JVM_12 in range)
    assertTrue(JvmTarget.JVM_13 in range)
    assertTrue(JvmTarget.JVM_14 in range)
    assertTrue(JvmTarget.JVM_15 in range)
    assertTrue(JvmTarget.JVM_16 in range)
    assertTrue(JvmTarget.JVM_17 in range)
    assertFalse(JvmTarget.JVM_18 in range)
    assertFalse(JvmTarget.JVM_19 in range)
    assertFalse(JvmTarget.JVM_20 in range)
    assertFalse(JvmTarget.JVM_21 in range)
    assertTrue(JvmTarget.JVM_1_8 !in range)
  }
}