package org.podval.tools.build

import org.gradle.api.Project
import scala.jdk.CollectionConverters.IterableHasAsScala

final class ClassPathAddition(
  configurationName: String,
  classPathConfigurationName: String
):
  def apply(
    project: Project
  ): ClassLoader = GradleClassPath.addTo(
    obj = this, 
    files = SourceSets.getConfiguration(project, configurationName).asScala
  )
  
  def verify(
    project: Project,
    scalaLibrary: ScalaLibrary
  ): Unit = scalaLibrary.verify(SourceSets.getConfiguration(project, classPathConfigurationName))

object ClassPathAddition:
  final class Many(classPathAdditions: Seq[ClassPathAddition]):
    def apply(
      project: Project,
      scalaLibrary: ScalaLibrary
    ): Unit =
      classPathAdditions.foreach(_.apply(project))
      classPathAdditions.foreach(_.verify(project, scalaLibrary))
