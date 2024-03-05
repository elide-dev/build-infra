@file:Suppress("UnstableApiUsage")

package dev.elide.infra.gradle.baseline

import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import java.io.Closeable
import java.util.concurrent.ConcurrentSkipListMap
import java.util.stream.Stream
import kotlin.reflect.KClass
import kotlin.streams.asSequence

/**
 * # Aggregation: Target Service
 */
public abstract class AggregateTargetService : BuildService<AggregateTargetService.Params>, AutoCloseable {
  /**
   * ## Service Parameters
   */
  public interface Params : BuildServiceParameters {
    // Nothing at this time.
  }

  /**
   * ## Target Type
   */
  public interface TargetType<E> where E: Enum<E> {
    /** Label to show for this aggregation type */
    public val label: String

    /** Description to show for this aggregation type */
    public val description: String

    /** Description to show for this aggregation type */
    public val impl: KClass<*>
  }

  /**
   * ## Target Types: Standard
   */
  public enum class StandardTargetType (
    override val label: String,
    override val description: String,
    override val impl: KClass<*>,
  ) : TargetType<StandardTargetType> {
    /**
     * ### JVM Test Suite
     *
     * Describes a registered JVM test suite instance, which is eligible for aggregation for the purposes of test and
     * coverage reporting.
     */
    JVM_TEST_SUITE(
      "JVM Test Suite",
      "Aggregate test and coverage reporting artifacts",
      JvmTestSuite::class,
    )
  }

  // Registry of instances.
  @PublishedApi internal val registry: MutableMap<TargetType<*>, MutableMap<String, Any>> = ConcurrentSkipListMap()

  override fun close() {
    registry.values.flatMap { it.values }.map {
      when (it) {
        is Closeable -> { it.close() }
        is AutoCloseable -> { it.close() }
      }
    }
  }

  /**
   * ## All Target Types
   */
  public val allTypes: Sequence<TargetType<*>> get() = registry.keys.asSequence()

  /**
   * ## All Registered Instances
   */
  public val allInstances: Sequence<Pair<TargetType<*>, Any>> get() = registry.entries.asSequence().map {
    it.key to it.value
  }

  /**
   * ## Register Instance
   */
  public inline fun <reified V: Any, reified T: TargetType<T>> register(type: T, name: String, instance: V) {
    val target: MutableMap<String, Any> = if (type in registry) {
      registry[type]!!
    } else ConcurrentSkipListMap<String, Any>().also {
      registry[type] = it
    }
    require(name !in target) {
      "Instance at name '$name' already registered for type '$type'"
    }
    target[name] = instance
  }

  /**
   * ## Resolve Instances
   */
  public inline fun <reified V: Any> resolve(type: TargetType<*>): Sequence<V> {
    return when (val target = registry[type]) {
      null -> Stream.empty<V>().asSequence()
      else -> target.entries.stream().asSequence().map {
        requireNotNull(it.value) { "Cannot cast `null` registered instance" }
        requireNotNull(it.value as? V) { "Cast failed from type '${it.value::class.java}' to '${V::class.java.name}'" }
      }
    }
  }
}
