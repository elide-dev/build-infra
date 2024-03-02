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
