package org.podval.tools.backend

import org.gradle.api.{GradleException, Project}
import org.podval.tools.platform.IntelliJIdea

abstract class Logging(project: Project) extends WithProject(project):
  final def error(message: String): Nothing =
    throw GradleException(s"${pluginMessage(message)}\nDocumentation: https://github.com/dubinsky/scalajs-gradle")

  final def announce(message: String): Unit =
    info(s"$message${if !IntelliJIdea.runningIn then "" else " [IJ]"}")

  final def info(message: String): Unit =
    project.getLogger.info(pluginMessage(message), null, null, null)

  private def pluginMessage(message: String): String =
    s"Plugin 'org.podval.tools.scalajs' in $project: $message."
