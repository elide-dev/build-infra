package dev.elide.infra.gradle.base

import dev.elide.infra.gradle.api.Convention
import org.gradle.api.initialization.Settings
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.the
import kotlin.reflect.KClass

// Abstract convention for settings types.
public abstract class SettingsConvention<C> protected constructor (
  private val type: KClass<C>,
  factory: ObjectFactory,
) : AbstractConvention<C, Settings>(factory) where C: Convention<C, Settings> {
  override fun type(): KClass<C> = type
}
