package dev.elide.infra.gradle.base

import dev.elide.infra.gradle.api.Convention
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.PluginAware
import org.gradle.kotlin.dsl.the

// Abstract convention base class for all target types.
public abstract class AbstractConvention<C, T> protected constructor (
  @Suppress("UNUSED_PARAMETER") factory: ObjectFactory,
) : Convention<C, T> where C: Convention<C, T>, T: ExtensionAware, T: PluginAware {
  override fun T.invoke(op: C.() -> Unit) {
    the(type()).apply(op)
  }
}
