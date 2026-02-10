package org.podval.tools.build

import org.gradle.api.artifacts.Dependency as GDependency
import org.podval.tools.util.Strings.{prefix, split}
import java.io.File

final class Artifact(
  val group: Option[String],
  val name: String,
  val backend: Option[String],
  val scalaVersion: Option[String],
  val version: Option[String],
  val classifier: Option[String],
  val extension: Option[String]
):
  override def toString: String = dependencyNotation

  def fileName: String =
    nameToString +
    "-" +
    version.get +
    prefix("-", classifier) +
    "." +
    extension.getOrElse("jar")

  def dependencyNotation: String =
    group.get +
    ":" +
    nameToString +
    ":" +
    version.get +
    prefix(":", classifier) +
    prefix("@", extension)

  private def nameToString: String =
    name +
    prefix("_", backend) +
    prefix("_", scalaVersion)

object Artifact:
  def suffix(
    backend: Backend,
    scalaLibrary: ScalaLibrary
  ): String =
    s"${prefix("_", backend.artifactSuffix)}_${scalaLibrary.scalaBinaryVersionPrefix}"

  def fromFile(file: File): Artifact =
    val (nameAndVersion: String, extension: Option[String]) = split(file.getName, '.')
    val (name: String, version: Option[String]) = split(nameAndVersion, '-')
    from(
      group = None,
      nameString = name,
      version = version,
      classifier = None,
      extension = extension
    )

  def fromDependency(dependency: GDependency): Artifact =
    from(
      group = Option(dependency.getGroup),
      nameString = dependency.getName,
      version = Option(dependency.getVersion),
      classifier = None,
      extension = None
    )

  private def from(
    group: Option[String],
    nameString: String,
    version: Option[String],
    classifier: Option[String],
    extension: Option[String]
  ): Artifact =
    val (nameAndBackend: String, scalaVersion: Option[String]) = split(nameString, '_')
    val (name: String, backend: Option[String]) = split(nameAndBackend, '_')
    Artifact(
      group = group,
      name = name,
      backend = backend,
      scalaVersion = scalaVersion,
      version = version,
      classifier = classifier,
      extension = extension
    )
