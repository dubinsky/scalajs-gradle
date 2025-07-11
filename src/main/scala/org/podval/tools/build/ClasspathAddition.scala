package org.podval.tools.build

import org.gradle.api.Project
import org.podval.tools.gradle.{Configurations, GradleClasspath}
import scala.jdk.CollectionConverters.IterableHasAsScala

final class ClasspathAddition(configurationName: String):
  def apply(
    project: Project
  ): ClassLoader = GradleClasspath.addTo(
    filesToAdd = Configurations.configuration(project, configurationName).asScala
  )
  
  def verify(
    project: Project,
    scalaLibrary: ScalaLibrary
  ): Unit =
    scalaLibrary.verify(project)

object ClasspathAddition:
  final class Many(classPathAdditions: Seq[ClasspathAddition]):
    def apply(
      project: Project,
      scalaLibrary: ScalaLibrary
    ): Unit =
      classPathAdditions.foreach(_.apply(project))
      classPathAdditions.foreach(_.verify(project, scalaLibrary))
