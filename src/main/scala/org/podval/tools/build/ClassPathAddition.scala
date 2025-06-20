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
