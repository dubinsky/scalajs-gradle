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
  final def publishedFor(scalaVersion: ScalaVersion): ScalaVersion = 
    val binaryVersion: ScalaBinaryVersion = scalaVersion.binaryVersion

    if isPublishedFor(binaryVersion) then scalaVersion else ScalaBinaryVersion
      .all
      .dropWhile(_ != binaryVersion)
      .find(isPublishedFor)
      .map(_.scalaVersionDefault)
      .getOrElse(throw GradleException(s"Dependency $this is not published for ${scalaVersion.binaryVersion}"))

object ScalaDependencyMaker:
  abstract class Delegating(delegate: ScalaDependencyMaker)
    extends DependencyMaker.Delegating(delegate) with ScalaDependencyMaker:

    override def isPublishedFor(binaryVersion: ScalaBinaryVersion): Boolean = delegate.isPublishedFor(binaryVersion)
    override def isScalaVersionFull: Boolean = delegate.isScalaVersionFull

  trait NotPublishedForScala3 extends ScalaDependencyMaker:
    final override def isPublishedFor(binaryVersion: ScalaBinaryVersion): Boolean = binaryVersion match
      case ScalaBinaryVersion.Scala3 => false
      case _ => true
      