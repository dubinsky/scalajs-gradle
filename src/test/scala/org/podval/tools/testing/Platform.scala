package org.podval.tools.testing

import org.opentorah.build.{Dependency, JavaDependency, ScalaDependency, ScalaLibrary, Version}
import org.opentorah.node.NodeDependency
import org.podval.tools.testing.framework.FrameworkDescriptor

final class Platform(
  val scalaVersion: Version,
  val isScalaJS: Boolean,
  val nodeVersion: Version = NodeDependency.versionDefault
):
  private def flavour: String = if isScalaJS then "ScalaJS" else "Scala"
  
  def displayName: String = s"in $flavour v$scalaVersion" // TODO SCalaJS 3.2.2 is incorrect ;)

  def scalaLibrary: Dependency.WithVersion = ScalaLibrary.forVersion(scalaVersion)

  def getNodeVersion: Option[Version] = if !isScalaJS then None else Some(nodeVersion)
  
  def toDependency(framework: FrameworkDescriptor): Dependency.WithVersion =
    require(if isScalaJS then framework.isScalaJSSupported else framework.isScalaSupported)

    val group: String = framework.group
    val artifact: String = framework.artifact

    (if !framework.isScalaDependency then JavaDependency(group, artifact) else (
      if scalaVersion.major == ScalaLibrary.Scala3.versionMajor
      then ScalaDependency.Scala3(group, artifact, isScalaJS = isScalaJS)
      else ScalaDependency.Scala2(group, artifact, isScalaJS = isScalaJS)
    ).withScalaVersion(scalaVersion)).withVersion(Version(framework.versionDefault))
