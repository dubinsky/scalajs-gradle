package org.podval.tools.build

final class ScalaPlatform(
  val scalaVersion: Version,
  val backendKind: ScalaBackendKind
):
  def version: ScalaVersion = ScalaVersion.forVersion(scalaVersion)

  def withBackend(backendKind: ScalaBackendKind): ScalaPlatform = ScalaPlatform(
    scalaVersion,
    backendKind
  )

  def toScala2: ScalaPlatform =
    if !version.isScala3 then this else ScalaPlatform(
      ScalaVersion.Scala2.majorAndMinor,
      backendKind
    )
