package org.podval.tools.scalajs

import org.gradle.api.Task
import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.opentorah.build.Gradle.*

trait ScalaJSTask extends Task:
  setDescription(s"$flavour ScalaJS")

  protected def flavour: String

  // Note: If dynamically-loaded classes are mentioned in the Task and Extension subclasses,
  // Gradle decorating code breaks at the plugin load time.
  // Such code should be in a separate class, and even there dynamically-loaded classes can not be mentioned (too much :)).
  final def act[T](action: Actions => T): T =
    addConfigurationToClassPath(this, ScalaBasePlugin.ZINC_CONFIGURATION_NAME)
    addConfigurationToClassPath(this, ScalaJS.configurationName)
    action(Actions(this))
