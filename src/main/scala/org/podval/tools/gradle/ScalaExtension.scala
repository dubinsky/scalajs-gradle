package org.podval.tools.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.scala.ScalaPluginExtension
import org.podval.tools.build.ScalaVersion

object ScalaExtension:
  def findScalaVersion(project: Project): Option[ScalaVersion] = find(project)
    .map(_.getScalaVersion)
    .map(_.getOrNull)
    .flatMap(Option(_))
    .map(ScalaVersion(_))

  def setScalaVersion(project: Project, version: ScalaVersion): Unit = find(project)
    .get
    .getScalaVersion
    .set(version.toString)

  private def find(project: Project): Option[ScalaPluginExtension] = Option(project
    .getExtensions
    .findByType(classOf[ScalaPluginExtension])
  )
