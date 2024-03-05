package dev.elide.infra.gradle.baseline

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * # Aggregation: Targets Plugin
 */
public class AggregateTargetPlugin : Plugin<Project> {
  internal companion object {
    const val SERVICE_NAME: String = "infraAggregatation"
    const val AGGREGATES_SHOW_TASK: String = "showAggregates"
  }

  override fun apply(target: Project) {
    val svc = target.gradle.sharedServices.registerIfAbsent(SERVICE_NAME, AggregateTargetService::class.java) {
      // no configuration to apply at this time
    }
    target.tasks.register(AGGREGATES_SHOW_TASK) {
      doLast {
        svc.get().allInstances.forEach { (type, target) ->
          logger.lifecycle("Registered instance (type: $type): $target")
        }
      }
    }
  }
}
