package org.podval.tools.test.framework

import org.gradle.api.GradleException
import org.podval.tools.build.{Dependency, DependencyMaker, ScalaBackend, ScalaVersion, Version}
import org.podval.tools.util.Scala212Collections.{arrayConcat, arrayFind}

// Based on sbt.TestFramework.
trait FrameworkDescriptor extends DependencyMaker derives CanEqual:
  final override def toString: String = description

  def name: String
  def className: String
  def sharedPackages: List[String]
  def tagOptions: Option[TagOptions]
  def usesTestSelectorAsNestedTestSelector: Boolean
  def additionalOptions(isRunningInIntelliJ: Boolean): Array[String] = Array.empty
  def maker(backend: ScalaBackend): Option[DependencyMaker] = None
  def underlying(backend: ScalaBackend): Option[DependencyMaker] = None

  final def args(
    isRunningInIntelliJ: Boolean,
    includeTags: Array[String],
    excludeTags: Array[String]
  ): Array[String] = arrayConcat(
    additionalOptions(isRunningInIntelliJ),
    tagOptions
      .map(_.args(includeTags, excludeTags))
      .getOrElse(Array.empty)
  )

  final def dependencyWithVersion(
    backend: ScalaBackend,
    scalaVersion: ScalaVersion,
    version: Option[Version]
  ): Dependency#WithVersion =
    val maker: DependencyMaker = this
      .maker(backend)
      .getOrElse(throw GradleException(s"Test framework $this does not support $backend."))
    maker
      .dependency(scalaVersion)
      .withVersion(version.getOrElse(maker.versionDefaultFor(backend, scalaVersion)))

object FrameworkDescriptor:
  private val all: Array[FrameworkDescriptor] = Array(
    JUnit4,
    JUnit4ScalaJS,
    JUnit4ScalaNative,
    MUnit,
    ScalaTest,
    ScalaCheck,
    Specs2,
    UTest,
    ZioTest
  )

  def forBackend(backend: ScalaBackend): List[FrameworkDescriptor] =
    all.toList.filter(_.maker(backend).isDefined)

  def forName(name: String): FrameworkDescriptor =
    arrayFind(all, _.name == name)
      .getOrElse(throw GradleException(s"Test framework descriptor for '$name' not found"))

  def forClass(clazz: Class[? <: FrameworkDescriptor]): FrameworkDescriptor =
    arrayFind(all, _.getClass.getName.startsWith(clazz.getName))
      .getOrElse(throw GradleException(s"Test framework descriptor for '$clazz' not found"))
