package org.podval.tools.build

import org.podval.tools.backend.ScalaBackend
import org.podval.tools.util.Strings

final class ScalaDependency private(
  scalaBackend: ScalaBackend,
  override val group: String,
  override val artifact: String,
  isScalaVersionFull: Boolean,
  override val isVersionCompound: Boolean
) extends FindableDependency[ScalaDependency.WithScalaVersion]:
  override def classifier(version: Version): Option[String] = None
  override def extension(version: Version): Option[String] = None

  def withScalaVersion(scalaVersion: ScalaVersion): ScalaDependency.WithScalaVersion = ScalaDependency.WithScalaVersion(
    findable = this,
    scalaVersion
  )

  def artifactNameSuffix(scalaVersion: ScalaVersion): String =
    val versionSuffix: Version =
      if isScalaVersionFull
      then scalaVersion.version
      else scalaVersion.binaryVersion.versionSuffix

    s"${scalaBackend.artifactSuffixString}_$versionSuffix"

  override protected def dependencyForArtifactName(
    artifactName: String
  ): Option[ScalaDependency.WithScalaVersion] = artifactAndScalaVersion(artifactName)
    .flatMap: (artifact, scalaVersion) =>
      val matches: Boolean = artifact == this.artifact
      if !matches then None else Some(scalaVersion)
    .map(withScalaVersion)

  private def artifactAndScalaVersion(artifactName: String): Option[(String, ScalaVersion)] =
    val (artifactAndBackend: String, scalaVersionOpt: Option[String]) = Strings.split(artifactName, '_')
    val (artifact: String, backendSuffixOpt: Option[String]) = Strings.split(artifactAndBackend, '_')
    val matches: Boolean =
      (scalaBackend.artifactSuffixOpt == backendSuffixOpt) &&
      scalaVersionOpt.isDefined
    if !matches then None else Some((artifact, ScalaVersion(scalaVersionOpt.get)))

object ScalaDependency:
  final class WithScalaVersion(
    findable: ScalaDependency,
    scalaVersion: ScalaVersion
  ) extends Dependency(
    group = findable.group,
    artifact = findable.artifact
  ):
    override def classifier(version: Version): Option[String] = findable.classifier(version)
    override def extension(version: Version): Option[String] = findable.extension(version)
    override protected def artifactNameSuffix: String = findable.artifactNameSuffix(scalaVersion)
  
  trait Maker extends Dependency.Maker:
    def scala2: Boolean = false
    def isScalaVersionFull: Boolean = false
    def isVersionCompound: Boolean = false

    final override def findable: ScalaDependency = ScalaDependency(
      scalaBackend = scalaBackend,
      group = group,
      artifact = artifact,
      isScalaVersionFull = isScalaVersionFull,
      isVersionCompound = isVersionCompound
    )

    final override def dependency(scalaVersion: ScalaVersion): WithScalaVersion = findable.withScalaVersion(
      if !scala2 || !scalaVersion.isScala3
      then scalaVersion
      else
        // Scala 2 version used by Scala 3 from 3.0.0 to the current is 2.13.
        // Assuming the latest version is somewhat troubling though ;)
        ScalaBinaryVersion.Scala213.versionDefault
    )
