package org.podval.tools.testing

import org.podval.tools.build.{Dependency, JavaDependency, ScalaDependency, ScalaLibrary, Version}
import org.podval.tools.node.NodeDependency
import org.podval.tools.testing.framework.FrameworkDescriptor

final class Platform(
  val scalaVersion: Version,
  val isScalaJS: Boolean,
  val nodeVersion: Version = NodeDependency.versionDefault
):
  def displayName: String = s"""in Scala v$scalaVersion${if isScalaJS then " with ScalaJS" else ""}"""

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
