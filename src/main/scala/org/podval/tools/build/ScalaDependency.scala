package org.podval.tools.build

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

    s"${scalaPlatform.backend.suffixString}_$versionSuffix"

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
      (scalaPlatform.backend.suffix == backendSuffixOpt) &&
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

    // TODO - implement? (using ScalaPlatform.artifactAndScalaVersion())
    override protected def verifyRequiredMore(): Unit = ()
//    if found.dependency.isInstanceOf[Scala2Dependency] then
//      val scalaVersion: String = getScalaVersion(found.dependency.asInstanceOf[Scala2Dependency])
//      val version: Scala2Dependency.Version = found.version.asInstanceOf[Scala2Dependency.Version]
//      if version.scalaVersion != scalaVersion then project.getLogger.info(
//        s"Found $found, but the project uses Scala 2 version $scalaVersion", null, null, null
//      )

  trait Maker extends Dependency.Maker[ScalaPlatform]:
    def jvm: Boolean = false
    def scala2: Boolean = false
    def isScalaVersionFull: Boolean = false
    def scalaVersion(scalaPlatform: ScalaPlatform): Version = scalaPlatform.scalaVersion

    private def adjusted(scalaPlatform: ScalaPlatform): ScalaPlatform =
      def adjustForScala2(scalaPlatform: ScalaPlatform): ScalaPlatform =
        if !scala2
        then scalaPlatform
        else scalaPlatform.toScala2

      def adjustForJvm(scalaPlatform: ScalaPlatform) =
        if !jvm
        then scalaPlatform
        else scalaPlatform.toJvm

      adjustForJvm(adjustForScala2(scalaPlatform))

    final override def findable(scalaPlatform: ScalaPlatform): ScalaDependency = ScalaDependency(
      scalaPlatform = adjusted(scalaPlatform),
      group,
      artifact,
      isScalaVersionFull
    )

    final override def dependency(scalaPlatform: ScalaPlatform): WithScalaVersion =
      findable(scalaPlatform).withScalaVersion(scalaVersion(adjusted(scalaPlatform)))


  trait MakerScala2Jvm extends Maker:
    final override def jvm: Boolean = true
    final override def scala2: Boolean = true
