package org.podval.tools.gradle

import org.gradle.api.artifacts.{Configuration, Dependency}
import org.podval.tools.util.Strings
import java.io.File
import scala.jdk.CollectionConverters.SetHasAsScala

object Dependencies:
  final class DependencyData(
    val version: Option[String],
    val group: Option[String],
    val artifactName: String,
    val classifier: Option[String],
    val extension: Option[String]
  )

  def forConfiguration[T](
    configuration: Configuration, 
    f: DependencyData => Option[T]
  ): Option[T] = find(f, configuration
    .getDependencies
    .asScala
    .toSet
    .map((dependency: Dependency) => DependencyData(
      version = Option(dependency.getVersion),
      group = Option(dependency.getGroup),
      artifactName = dependency.getName,
      classifier = None,
      extension = Some("jar")
    ))
  )

  def forClasspath[T](
    classpath: Iterable[File],
    f: DependencyData => Option[T]
  ): Option[T] = find(f, classpath
    .map((file: File) =>
      val (nameAndVersion: String, extension: Option[String]) = Strings.split(file.getName, '.')
      val (name: String, version: Option[String]) = Strings.split(nameAndVersion, '-')
      DependencyData(
        version = version,
        group = None,
        artifactName = name,
        classifier = None,
        extension = extension
      )
    )
  )

  private def find[T](
    f: DependencyData => Option[T],
    data: Iterable[DependencyData]
  ): Option[T] = data.flatMap(f).headOption
