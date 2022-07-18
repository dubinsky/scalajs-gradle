package org.podval.tools.scalajs

import org.gradle.api.Task
import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.opentorah.build.Gradle.*
import scala.jdk.CollectionConverters.*

trait ScalaJSTask extends Task:
  setDescription(s"$flavour ScalaJS")

  protected def flavour: String

  // Note: If dynamically-loaded classes are mentioned in the Task and Extension subclasses,
  // Gradle decorating code breaks at the plugin load time.
  // Such code should be in a separate class, and even there dynamically-loaded classes can not be mentioned (too much :)).
  final def expandClassPath(): Unit =
    addToClassPath(this, getProject.getConfiguration(ScalaBasePlugin.ZINC_CONFIGURATION_NAME).asScala)
    addToClassPath(this, getProject.getConfiguration(ScalaJS        .configurationName      ).asScala)
