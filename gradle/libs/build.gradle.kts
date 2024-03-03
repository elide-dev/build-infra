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

plugins {
  id("infra.root")
}

description = "Supplemental version catalogs and libraries for Gradle build infra"
group = "dev.elide.infra"

evaluationDependsOnChildren()

private fun Task.taskInAllLibs(task: String): Unit = listOf(
  projects.catalogCore,
  projects.catalogInfra,
  projects.catalogLibs,
).forEach {
  dependsOn(project(":${it.name}").tasks.named(task))
}

// Top-level tasks.

val clean by tasks.registering { taskInAllLibs("clean") }
val test by tasks.registering { taskInAllLibs("test") }
val check by tasks.registering { taskInAllLibs("check") }
val build by tasks.registering { taskInAllLibs("build") }
