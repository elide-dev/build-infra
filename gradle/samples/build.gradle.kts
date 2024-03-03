
description = "Samples for testing embedded build infra plugins"

private fun Task.taskInAllSamples(name: String) {
  listOf(
    projects.samples.jmodLibrary,
  ).forEach {
    dependsOn(project(":${it.name}").tasks.named(name))
  }
}

// Top-level tasks.

val clean by tasks.registering { taskInAllSamples("clean") }
val build by tasks.registering { taskInAllSamples("build") }
val test by tasks.registering { taskInAllSamples("test") }
val check by tasks.registering { taskInAllSamples("check") }
