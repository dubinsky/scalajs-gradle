package org.podval.tools.testing

import org.podval.tools.build.{Dependency, JavaDependency, ScalaPlatform, Version}
import org.podval.tools.node.NodeDependency
import org.podval.tools.testing.framework.FrameworkDescriptor

final class Platform(
  val scalaVersion: Version,
  val isScalaJS: Boolean,
  val nodeVersion: Version = NodeDependency.versionDefault
):
  def displayName: String = s"""in Scala v$scalaVersion${if isScalaJS then " with ScalaJS" else ""}"""
  
  def getNodeVersion: Option[Version] = if !isScalaJS then None else Some(nodeVersion)
  
  def toDependency(framework: FrameworkDescriptor): Dependency.WithVersion =
    require(if isScalaJS then framework.isScalaJSSupported else framework.isScalaSupported)

    val group: String = framework.group
    val artifact: String = framework.artifact

    val result: Dependency = 
      if !framework.isScalaDependency
      then JavaDependency(group, artifact)
      else ScalaPlatform
        .get(scalaVersion, isScalaJS)
        .dependency(group, artifact)
        .withScalaVersion(scalaVersion)

    result.withVersion(Version(framework.versionDefault))
