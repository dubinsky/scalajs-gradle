package org.podval.tools.build

import org.gradle.api.GradleException

trait ScalaDependencyMaker extends DependencyMaker:
  def isPublishedFor(binaryVersion: ScalaBinaryVersion): Boolean = true
  def isScalaVersionFull: Boolean = false

  final override def classifier(version: PreVersion): Option[String] = None
  final override def extension(version: PreVersion): Option[String] = None
  final override def findable: ScalaDependencyFindable = ScalaDependencyFindable(this)

  final override def dependency(scalaVersion: ScalaVersion): ScalaDependency =
    findable.withScalaVersion(publishedFor(scalaVersion))

  // Scala 2 version used by Scala 3 from 3.0.0 to the current is 2.13.
  // Assuming the latest version is somewhat troubling though ;)
  private def publishedFor(scalaVersion: ScalaVersion): ScalaVersion =
    val binaryVersion: ScalaBinaryVersion = scalaVersion.binaryVersion

    if isPublishedFor(binaryVersion) then scalaVersion else ScalaBinaryVersion
      .all
      .dropWhile(_ != binaryVersion)
      .find(isPublishedFor)
      .map(_.scalaVersionDefault)
      .getOrElse(throw GradleException(s"Dependency $this is not published for ${scalaVersion.binaryVersion}"))

object ScalaDependencyMaker:
  trait Scala3 extends ScalaDependencyMaker:
    final override def isPublishedFor(binaryVersion: ScalaBinaryVersion): Boolean = binaryVersion == ScalaBinaryVersion.Scala3

  trait Scala2 extends ScalaDependencyMaker:
    final override def isPublishedFor(binaryVersion: ScalaBinaryVersion): Boolean = binaryVersion != ScalaBinaryVersion.Scala3

  trait Jvm extends ScalaDependencyMaker with DependencyMaker.Jvm

  trait JvmScala2 extends Scala2 with Jvm

  trait IsScalaVersionFull extends ScalaDependencyMaker:
    final override def isScalaVersionFull: Boolean = true
