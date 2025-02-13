package org.podval.tools.testing

import org.podval.tools.build.{ScalaPlatform, Version}
import org.podval.tools.node.NodeDependency

final class Platform private(
  val scalaPlatformWithScalaVersion: ScalaPlatform.WithScalaVersion,
  val nodeVersion: Version
):
  def isScalaJS: Boolean = scalaPlatformWithScalaVersion.isScalaJS

object Platform:
  def apply(
    scalaVersion: Version,
    isScalaJS: Boolean,
    nodeVersion: Version = NodeDependency.versionDefault
  ): Platform = new Platform(
    scalaPlatformWithScalaVersion = ScalaPlatform.WithScalaVersion(
      scalaVersion,
      isScalaJS
    ),
    nodeVersion
  )