package org.podval.tools.testing

import org.opentorah.build.{Dependency, JavaDependency, Scala2Dependency, Scala3Dependency, ScalaLibrary}
import org.opentorah.node.NodeDependency
import org.podval.tools.testing.framework.FrameworkDescriptor

final class Platform(
  val scalaVersion: String,
  val isScalaJSDisabled: Boolean,
  val nodeVersion: String = Platform.nodeVersionDefault
):
  private def isScala3: Boolean = scalaVersion.startsWith("3")

  private def flavour: String = if isScalaJSDisabled then "Scala" else "ScalaJS"
  
  def displayName: String = s"in $flavour v$scalaVersion" // TODO SCalaJS 3.2.2 is incorrect ;)

  def scalaLibrary: Dependency.WithVersion = (if isScala3 then ScalaLibrary.Scala3 else ScalaLibrary.Scala2)(scalaVersion)

  def getNodeVersion: Option[String] = if isScalaJSDisabled then None else Some(nodeVersion)
  
  def toDependency(framework: FrameworkDescriptor): Dependency.WithVersion =
    require(if isScalaJSDisabled then framework.isScalaSupported else framework.isScalaJSSupported)

    val group: String = framework.group
    val artifact: String = framework.artifact + (if isScalaJSDisabled then "" else "_sjs1")
    val version: String = framework.versionDefault

    if !framework.isScalaDependency
    then JavaDependency(group, artifact)(version)
    else if isScala3
    then Scala3Dependency(group, artifact)(scalaVersion, version)
    else Scala2Dependency(group, artifact)(scalaVersion, version)

object Platform:
  // TODO use defaults from opentorah
  val scala3VersionDefault: String = "3.2.2"
  val scala213VersionDefault: String = "2.13.10"
  val scala212VersionDefault: String = "2.12.17"
  val nodeVersionDefault: String = NodeDependency.versionDefault
