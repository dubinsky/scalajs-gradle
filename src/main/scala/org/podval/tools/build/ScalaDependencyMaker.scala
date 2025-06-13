package org.podval.tools.build

trait ScalaDependencyMaker extends DependencyMaker:
  final override def classifier(version: PreVersion): Option[String] = None

  final override def extension(version: PreVersion): Option[String] = None

  def isPublishedForScala3: Boolean = true

  def isPublishedForScala213: Boolean = true

  def isScalaVersionFull: Boolean = false

  final override def findable: ScalaDependencyFindable = ScalaDependencyFindable(this)

  final override def dependency(scalaVersion: ScalaVersion): ScalaDependency =
    findable.withScalaVersion(adjust(scalaVersion))

  private def adjust(scalaVersion: ScalaVersion) =
    if scalaVersion.isScala3 then
      if isPublishedForScala3 then scalaVersion else
        // Scala 2 version used by Scala 3 from 3.0.0 to the current is 2.13.
        // Assuming the latest version is somewhat troubling though ;)
        if isPublishedForScala213
        then ScalaBinaryVersion.Scala213.versionDefault
        else ScalaBinaryVersion.Scala212.versionDefault
    else if scalaVersion.version.minor == ScalaBinaryVersion.Scala213.versionMinor then
      if isPublishedForScala213
      then scalaVersion
      else ScalaBinaryVersion.Scala212.versionDefault
    else
      scalaVersion
