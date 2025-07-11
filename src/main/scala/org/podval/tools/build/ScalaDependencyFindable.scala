package org.podval.tools.build

import org.podval.tools.util.Strings

final class ScalaDependencyFindable(
  val maker: ScalaDependencyMaker
) extends DependencyFindable[ScalaDependency]:

  def withScalaVersion(
    scalaVersion: ScalaVersion
  ): ScalaDependency = ScalaDependency(
    findable = this,
    scalaVersion
  )

  override protected def dependencyForArtifactName(
    artifactName: String
  ): Option[ScalaDependency] =
    val (artifactAndBackend: String, scalaVersionOpt: Option[String]) = Strings.split(artifactName, '_')
    val (artifact: String, backendSuffixOpt: Option[String]) = Strings.split(artifactAndBackend, '_')
    scalaVersionOpt
      .map(Version(_))
      .map(_.toScalaVersion)
      .flatMap: (scalaVersion: ScalaVersion) =>
        val matches: Boolean =
          (artifact == maker.artifact) &&
          (backendSuffixOpt == maker.scalaBackend.artifactSuffix)
  
        if !matches then None else Some(withScalaVersion(scalaVersion))
