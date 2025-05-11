package org.podval.tools.scalajsplugin

import org.gradle.api.Project
import org.podval.tools.build.{Gradle, GradleClassPath, ScalaLibrary}
import java.io.File
import scala.jdk.CollectionConverters.IterableHasAsScala

final class AddToClassPath(
  configurationName: String,
  projectScalaLibrary: ScalaLibrary,
  runtimeClasspathConfigurationName: String
):
  def add(project: Project): Unit = GradleClassPath.addTo(
    this,
    Gradle.getConfiguration(project, configurationName).asScala
  )
    
  def verify(project: Project) : Unit = projectScalaLibrary.verify(
    runtimeClasspathConfigurationName,
    ScalaLibrary.getFromClasspath(Gradle.getConfiguration(project, runtimeClasspathConfigurationName).asScala)
  )  
