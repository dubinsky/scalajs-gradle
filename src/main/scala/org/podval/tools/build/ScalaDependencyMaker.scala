package org.podval.tools.build

import org.gradle.api.GradleException
import org.podval.tools.jvm.JvmBackend

trait ScalaDependencyMaker extends DependencyMaker:
  def isPublishedFor(scalaVersion: ScalaVersion): Boolean = true
  def isScalaVersionFull: Boolean = false

  final override def classifier(version: PreVersion): Option[String] = None
  final override def extension(version: PreVersion): Option[String] = None
  final override def findable: ScalaDependencyFindable = ScalaDependencyFindable(this)

  final override def dependency(scalaLibrary: ScalaLibrary): ScalaDependency = findable.withScalaVersion(
    scalaLibrary.scala3.filter(isPublishedFor)
      .orElse(Some(scalaLibrary.scala2).filter(isPublishedFor))
      .getOrElse(throw GradleException(s"Dependency $this is not published for $scalaLibrary."))
  )

  private abstract class Wrapper extends ScalaDependencyMaker:
    private def delegate: ScalaDependencyMaker = ScalaDependencyMaker.this
    override def scalaBackend: ScalaBackend = delegate.scalaBackend
    override def group: String = delegate.group
    override def artifact: String = delegate.artifact
    override def versionDefault: Version = delegate.versionDefault
    override def versionDefaultFor(backend: ScalaBackend, scalaLibrary: ScalaLibrary): Version = 
      delegate.versionDefaultFor(backend, scalaLibrary)
    override def description: String = delegate.description
    override def isVersionCompound: Boolean = delegate.isVersionCompound
    override def isDependencyRequirementVersionExact: Boolean = delegate.isDependencyRequirementVersionExact
    override def isPublishedFor(scalaVersion: ScalaVersion): Boolean = delegate.isPublishedFor(scalaVersion)
    override def isScalaVersionFull: Boolean = delegate.isScalaVersionFull

  final def withGroup(groupId: String): ScalaDependencyMaker = new Wrapper:
    final override def group: String = groupId

  final def withVersionDefault(version: Version): ScalaDependencyMaker = new Wrapper:
    final override def versionDefault: Version = version

  final def withVersionCompound: ScalaDependencyMaker = new Wrapper:
    final override def isVersionCompound: Boolean = true

  final def scala3: ScalaDependencyMaker = new Wrapper:
    final override def isPublishedFor(scalaVersion: ScalaVersion): Boolean = scalaVersion.isScala3

  final def scala2: ScalaDependencyMaker = new Wrapper:
    final override def isPublishedFor(scalaVersion: ScalaVersion): Boolean = scalaVersion.isScala2

  final def withBackend(backend: ScalaBackend): ScalaDependencyMaker = new Wrapper:
    final override def scalaBackend: ScalaBackend = backend

  final def jvm: ScalaDependencyMaker = withBackend(JvmBackend)

  final def scalaCompilerPlugin: ScalaDependencyMaker = jvm.withScalaVersionFull

  final def withScalaVersionFull: ScalaDependencyMaker = new Wrapper:
    final override def isScalaVersionFull: Boolean = true

  final def withDependencyRequirementVersionExact: ScalaDependencyMaker = new Wrapper:
    final override def isDependencyRequirementVersionExact: Boolean = true
