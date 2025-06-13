package org.podval.tools.build

import org.gradle.api.Project

final class ExpandClassPath(addToClassPath: Seq[AddConfigurationToClassPath]):
  def apply(
    project: Project,
    scalaLibrary: ScalaLibrary
  ): Unit =
    addToClassPath.foreach(_.add(project))
    addToClassPath.foreach(_.verify(project, scalaLibrary))
