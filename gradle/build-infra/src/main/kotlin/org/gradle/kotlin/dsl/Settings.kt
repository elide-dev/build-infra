@file:Suppress("UnstableApiUsage")

package org.gradle.kotlin.dsl

import dev.elide.infra.gradle.api.ElideSettings
import org.gradle.api.initialization.Settings

/**
 * ## Settings: Infra Setup
 *
 * Setup conventions at settings time for a build infrastructure module.
 *
 * @param op Function to execute to build settings-time conventions
 */
public fun Settings.infra(op: ElideSettings.() -> Unit) {
  pluginManager.withPlugin("elide.settings") {
    the<ElideSettings>().apply(op)
  }
}
