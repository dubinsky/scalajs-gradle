package org.podval.tools.build

import org.gradle.api.artifacts.Dependency as GDependency
import org.podval.tools.platform.Strings.{prefix, split}
import java.io.File

final class Artifact(
  val group: Option[String],
  val name: Artifact.Name,
  val version: Option[String],
  val classifier: Option[String],
  val extension: Option[String]
):
  override def toString: String = dependencyNotation

  def fileName: String =
    name.toString +
    "-" +
    version.get +
    prefix("-", classifier) +
    "." +
    extension.getOrElse("jar")

  def dependencyNotation: String =
    group.get +
    ":" +
    name.toString +
    ":" +
    version.get +
    prefix(":", classifier) +
    prefix("@", extension)

object Artifact:
  final class Name(
    val name: String,
    val backend: Option[String],
    val scalaVersion: Option[String]
  ):
    override def toString: String =
      name +
      prefix("_", backend) +
      prefix("_", scalaVersion)

  def suffix(
    backend: Backend,
    scalaLibrary: ScalaLibrary
  ): String =
    s"${prefix("_", backend.artifactSuffix)}_${scalaLibrary.scalaBinaryVersionPrefix}"

  private def parseName(string: String): Name =
    val (nameAndBackend: String, scalaVersion: Option[String]) = split(string, '_')
    val (name: String, backend: Option[String]) = split(nameAndBackend, '_')
    new Name(
      name = name,
      backend = backend,
      scalaVersion = scalaVersion
    )

  def fromFile(file: File): Artifact =
    val (nameAndVersion: String, extension: Option[String]) = split(file.getName, '.')
    val (name: String, version: Option[String]) = split(nameAndVersion, '-')
    Artifact(
      group = None,
      name = parseName(name),
      version = version,
      classifier = None,
      extension = extension
    )

  def fromDependency(dependency: GDependency): Artifact =
    Artifact(
      group = Option(dependency.getGroup),
      name = parseName(dependency.getName),
      version = Option(dependency.getVersion),
      classifier = None,
      extension = None
    )
