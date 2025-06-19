package org.podval.tools.build

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import scala.jdk.CollectionConverters.IterableHasAsScala

final class ClassPathAddition(
  val configurationName: String,
  val classPathConfigurationName: String
):
  def apply(
    project: Project
  ): ClassLoader = GradleClassPath.addTo(
    this, project.getConfigurations.getByName(configurationName).asScala
  )
  
  def verify(
    project: Project,
    scalaLibrary: ScalaLibrary
  ): Unit = scalaLibrary.verify(project.getConfigurations.getByName(classPathConfigurationName))

object ClassPathAddition:
  final class Many(classPathAdditions: Seq[ClassPathAddition]):
    def apply(
      project: Project,
      scalaLibrary: ScalaLibrary
    ): Unit =
      classPathAdditions.foreach(_.apply(project))
      classPathAdditions.foreach(_.verify(project, scalaLibrary))
