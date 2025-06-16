package org.podval.tools.build

import org.gradle.api.Project

final class ClassPathAdditions(classPathAdditions: Seq[ClassPathAddition]):
  def apply(
    project: Project,
    scalaLibrary: ScalaLibrary
  ): Unit =
    classPathAdditions.foreach(_.apply(project))
    classPathAdditions.foreach(_.verify(project, scalaLibrary))
