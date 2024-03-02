package dev.elide.infra.gradle.api

import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.PluginAware

/**
 * # Elide Extension: Conventions
 */
public sealed interface ElideExtension<E, T> : Convention<E, T>
  where E: ElideExtension<E, T>,
        T: ExtensionAware,
        T: PluginAware
