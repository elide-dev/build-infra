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

package dev.elide.infra.gradle.jmod

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import javax.inject.Inject

/**
 * # JMod: Actions
 */
public enum class JModAction (internal val action: String) {
  /** Create a `jmod` artifact; the default. */
  CREATE("create");
}

/**
 * # Task: JMod Build
 */
public abstract class JModTask @Inject constructor (factory: ObjectFactory) : Exec() {
  @get:OutputFile public val outFile: RegularFileProperty = factory.fileProperty()

  @get:OutputDirectory public val destinationDirectory: DirectoryProperty = factory.directoryProperty()

  @get:Input public val action: Property<JModAction> = factory.property(JModAction::class.java)
    .convention(JModAction.CREATE)
}
