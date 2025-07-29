package org.podval.tools.build

import org.podval.tools.util.Strings.prefix

abstract class Dependency:
  def maker: DependencyMaker

  def artifactNameSuffix: String

  final def withVersion(
    scalaLibrary: ScalaLibrary,
    backend: ScalaBackend,
    version: Option[Version]
  ): Dependency#WithVersion =
    val versionEffective: Version = version.getOrElse(maker.versionDefaultFor(backend, scalaLibrary))
    withVersion(versionEffective)

  final def withVersion(version: PreVersion): WithVersion = WithVersion(version)

  final class WithVersion(val version: PreVersion):
    override def toString: String = dependencyNotation

    def dependencyNotation: String =
      s"${maker.group}:$artifactName:$version${prefix(":", classifier)}${prefix("@", extension)}"

    def fileName: String =
      s"$artifactName-$version${prefix("-", classifier)}.${extension.getOrElse("jar")}"

    private def artifactName: String = s"${maker.artifact}$artifactNameSuffix"
    private def classifier: Option[String] = maker.classifier(version)
    private def extension: Option[String] = maker.extension(version)
