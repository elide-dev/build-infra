package dev.elide.infra.gradle.base

import dev.elide.infra.gradle.api.Convention
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import kotlin.reflect.KClass

// Abstract convention for project types.
public abstract class ProjectConvention<C> protected constructor (
  private val type: KClass<C>,
  factory: ObjectFactory,
) : AbstractConvention<C, Project>(factory) where C: Convention<C, Project> {
  override fun type(): KClass<C> = type
}
