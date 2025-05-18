package org.podval.tools.build

final class ScalaPlatform(
  val scalaVersion: Version,
  val backend: ScalaBackend
):
  def version: ScalaVersion = ScalaVersion.forVersion(scalaVersion)

  def withBackend(backend: ScalaBackend): ScalaPlatform = ScalaPlatform(
    scalaVersion,
    backend
  )

  def toScala2: ScalaPlatform =
    if !version.isScala3 then this else ScalaPlatform(
      ScalaVersion.Scala2.majorAndMinor,
      backend
    )
