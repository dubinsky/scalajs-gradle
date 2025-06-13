package org.podval.tools.build

import org.podval.tools.util.Strings

final class ScalaDependency(
  findable: ScalaDependencyFindable,
  scalaVersion: ScalaVersion
) extends Dependency:
  override def maker: ScalaDependencyMaker = findable.maker
  
  override def artifactNameSuffix: String =
    val versionSuffix: PreVersion =
      if maker.isScalaVersionFull
      then scalaVersion.version
      else scalaVersion.binaryVersion.versionSuffix

    s"${maker.scalaBackend.artifactSuffixString}_$versionSuffix"
