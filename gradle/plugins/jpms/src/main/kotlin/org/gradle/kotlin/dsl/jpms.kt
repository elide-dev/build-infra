package org.gradle.kotlin.dsl

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 * Accessor for the JPMS module path configuration.
 */
public val Project.modulepath: Configuration get() = configurations.named("modulepath").get()
