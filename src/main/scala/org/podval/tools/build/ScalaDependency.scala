package org.podval.tools.build

import org.gradle.api.artifacts.Configuration
import org.podval.tools.util.Strings

final class ScalaDependency private(
  scalaPlatform: ScalaPlatform,
  override val group: String,
  override val artifact: String,
  isScalaVersionFull: Boolean
) extends FindableDependency[ScalaDependency.WithScalaVersion]:
  override def classifier(version: Version): Option[String] = None
  override def extension(version: Version): Option[String] = None

  def withScalaVersion(scalaVersion: Version): ScalaDependency.WithScalaVersion =
    require(
      requirement = scalaPlatform.version.isScalaVersionAcceptable(scalaVersion),
      message = s"Scala version $scalaVersion is not acceptable!"
    )
    ScalaDependency.WithScalaVersion(
      findable = this,
      scalaVersion
    )

  def artifactNameSuffix(scalaVersion: Version): String =
    val versionSuffix: String =
      if isScalaVersionFull
      then scalaVersion.toString
      else scalaPlatform.version.versionSuffix(scalaVersion)

    s"${scalaPlatform.backendKind.suffixString}_$versionSuffix"

  override protected def dependencyForArtifactName(
    artifactName: String
  ): Option[ScalaDependency.WithScalaVersion] = artifactAndScalaVersion(artifactName)
    .flatMap: (artifact, scalaVersion) =>
      val matches: Boolean =
        (artifact == this.artifact) &&
        scalaPlatform.version.isScalaVersionAcceptable(scalaVersion)
      if !matches then None else Some(scalaVersion)
    .map(withScalaVersion)

  private def artifactAndScalaVersion(artifactName: String): Option[(String, Version)] =
    val (artifactAndBackend: String, scalaVersionOpt: Option[String]) = Strings.split(artifactName, '_')
    val (artifact: String, backendSuffixOpt: Option[String]) = Strings.split(artifactAndBackend, '_')
    val matches: Boolean =
      (scalaPlatform.backendKind.suffix == backendSuffixOpt) &&
      scalaVersionOpt.isDefined
    if !matches then None else Some((artifact, Version(scalaVersionOpt.get)))

object ScalaDependency:
  final class WithScalaVersion(
    findable: ScalaDependency,
    scalaVersion: Version
  ) extends Dependency(
    group = findable.group,
    artifact = findable.artifact
  ):
    override def classifier(version: Version): Option[String] = findable.classifier(version)
    override def extension(version: Version): Option[String] = findable.extension(version)
    override protected def artifactNameSuffix: String = findable.artifactNameSuffix(scalaVersion)
  
  trait Maker extends Dependency.Maker[ScalaPlatform]:
    def backendKind: Option[ScalaBackendKind] = None
    def scala2: Boolean = false
    def isScalaVersionFull: Boolean = false

    final override def findable(scalaPlatform: ScalaPlatform): ScalaDependency = ScalaDependency(
      scalaPlatform = adjusted(scalaPlatform),
      group,
      artifact,
      isScalaVersionFull
    )

    final def findInConfiguration(
      scalaPlatform: ScalaPlatform,
      configuration: Configuration
    ): Option[Dependency.WithVersion] = 
      findable(scalaPlatform).findInConfiguration(configuration)
  
    final override def dependency(scalaPlatform: ScalaPlatform): WithScalaVersion =
      findable(scalaPlatform).withScalaVersion(adjusted(scalaPlatform).scalaVersion)

    private def adjusted(scalaPlatform: ScalaPlatform): ScalaPlatform =
      def adjustForScala2(scalaPlatform: ScalaPlatform): ScalaPlatform =
        if !scala2
        then scalaPlatform
        else scalaPlatform.toScala2
  
      def adjustForBackend(scalaPlatform: ScalaPlatform) = backendKind
        .map(scalaPlatform.withBackend)
        .getOrElse(scalaPlatform)
  
      adjustForBackend(adjustForScala2(scalaPlatform))

  trait MakerJvm extends Maker:
    final override def backendKind: Option[ScalaBackendKind] = Some(ScalaBackendKind.JVM)

  trait MakerScala2 extends Maker:
    final override def scala2: Boolean = true

  trait MakerScala2Jvm extends MakerJvm with MakerScala2
