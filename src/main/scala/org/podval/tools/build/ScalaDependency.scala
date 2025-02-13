package org.podval.tools.build

import org.gradle.api.Project

final class ScalaDependency(
  override val group: String,
  override val artifact: String,
  scalaPlatform: ScalaPlatform,
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
    scalaPlatform
      .artifactNameSuffix(
        isScalaVersionFull,
        scalaVersion
      )

  override protected def dependencyForArtifactName(
    artifactName: String
  ): Option[ScalaDependency.WithScalaVersion] = scalaPlatform
    .scalaVersionForArtifactName(
      artifactName,
      artifactNameExpected = artifact
    )
    .map(withScalaVersion)

  def required(
    version: Version,
    scalaLibrary: ScalaLibrary,
    reason: String,
    configurationName: String,
    isVersionExact: Boolean = false
  ): DependencyRequirement = DependencyRequirement(
    findable = this,
    dependency = withScalaVersion(scalaPlatform.version.scalaVersion(scalaLibrary)),
    version,
    reason,
    configurationName,
    isVersionExact
  )

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
