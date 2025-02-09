/*
 * Copyright (c) 2024 Elide Technologies, Inc.
 *
 * Licensed under the MIT license (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   https://opensource.org/license/mit/
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License.
 */

description = "Samples for testing embedded build infra plugins"

private fun Task.taskInAllSamples(name: String) {
  listOf(
    projects.samples.jmodKotlin,
    projects.samples.jmodLibrary,
    projects.samples.mrjarPurejava,
  ).forEach {
    dependsOn(project(":${it.name}").tasks.named(name))
  }
}

// Top-level tasks.

val clean by tasks.registering { taskInAllSamples("clean") }
val build by tasks.registering { taskInAllSamples("build") }
val test by tasks.registering { taskInAllSamples("test") }
val check by tasks.registering { taskInAllSamples("check") }
