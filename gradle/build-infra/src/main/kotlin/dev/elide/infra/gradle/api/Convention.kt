package dev.elide.infra.gradle.api

import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.PluginAware
import kotlin.reflect.KClass

/**
 * # Convention
 *
 * Top-level concept of a convention, which is configurable via direct invocation.
 */
public interface Convention<Context, Target>
  where Context: Convention<Context, Target>,
        Target: ExtensionAware,
        Target: PluginAware {
  /**
   * ## API Type
   *
   * Return the class which describes this convention's configuration API
   *
   * @return API layout type
   */
  public fun type(): KClass<Context>

  /**
   * ## Configure
   *
   * Invoke this extension, configuring its context.
   *
   * @param op Operation to run within the context extension instance
   */
  public operator fun Target.invoke(op: Context.() -> Unit)
}
