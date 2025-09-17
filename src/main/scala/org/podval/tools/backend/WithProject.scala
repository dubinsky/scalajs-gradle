package org.podval.tools.backend

import org.gradle.api.{GradleException, Project}
import org.podval.tools.build.ScalaVersion
import org.podval.tools.gradle.Projects
import org.podval.tools.gradle.ScalaExtension

abstract class WithProject(val project: Project):
  final def name: String = Projects.projectDirName(project)

  final def is(candidate: Project): Boolean = Projects.projectDirName(candidate) == name

  final def error(message: String): Nothing =
    throw GradleException(s"${pluginMessage(message)}")
  
  final def info(message: String): Unit =
    project.getLogger.info(pluginMessage(message), null, null, null)
  
  private def pluginMessage(message: String): String =
    s"Plugin 'org.podval.tools.scalajs' in $project: $message.\nDocumentation: https://github.com/dubinsky/scalajs-gradle"
  
  final protected def getScalaVersionFromScalaExtension: ScalaVersion = ScalaExtension
    .findScalaVersion(project)
    .getOrElse(error(
      s"""Scala version data is not supported when Scala version is inferred from the Scala library dependency;
         |set Scala version on the Scala plugin's extension instead: `scala.scalaVersion=...`""".stripMargin
    ))
