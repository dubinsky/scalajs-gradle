package org.podval.tools.build

final class WithVersion(
  val dependency: Dependency,
  val version: Version.Pre,
  val scalaVersion: Option[Version]
):
  override def toString: String = dependencyNotation

  def dependencyNotation: String = artifact.dependencyNotation

  def fileName: String = artifact.fileName

  private def artifact: Artifact = Artifact(
    group = Some(dependency.group),
    name = Artifact.Name(
      name = dependency.artifact,
      backend = dependency.backendSuffix,
      scalaVersion = scalaVersion.map(_.toString)
    ),
    version = Some(version.toString),
    classifier = dependency.classifier(version.version),
    extension = dependency.extension(version.version)
  )
