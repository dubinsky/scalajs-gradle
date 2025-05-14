package org.podval.tools.build

import org.gradle.api.artifacts.Configuration
import scala.jdk.CollectionConverters.IterableHasAsScala

final class AddConfigurationToClassPath(
  val configuration: Configuration,
  val classPath: Configuration
):
  def add(): ClassLoader = GradleClassPath.addTo(this, configuration.asScala)
  def verify(scalaLibrary: ScalaLibrary): Unit = scalaLibrary.verify(classPath)
