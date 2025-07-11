package org.podval.tools.test.framework

import org.gradle.api.GradleException
import org.podval.tools.build.{Dependency, DependencyMaker, ScalaBackend, ScalaVersion, Version}
import org.podval.tools.util.Scala212Collections.{arrayConcat, arrayFind}

// Based on sbt.TestFramework.
abstract class FrameworkDescriptor(
  final val group: String,
  final val artifact: String,
  // Note: versionDefault is not a parameter to avoid circular initialization with NonJvmBackend.junit4;
  // alternative is to delay it there: `junit4: => FrameworkDescriptor`...
  final val description: String,

  final val name: String,
  final val className: String,
  final val sharedPackages: List[String],
  tagOptionStyle: OptionStyle = OptionStyle.NotSupported,
  includeTagsOption: String = "",
  excludeTagsOption: String = "",
  additionalOptions: Array[String] = Array.empty,
  final val usesTestSelectorAsNestedTestSelector: Boolean = false
) extends DependencyMaker derives CanEqual:

  final override def toString: String = description

  def maker(backend: ScalaBackend): Option[DependencyMaker] = None

  def underlying(backend: ScalaBackend): Option[DependencyMaker] = None

  final def args(
    includeTags: Array[String],
    excludeTags: Array[String]
  ): Array[String] = arrayConcat(additionalOptions, arrayConcat(
    tagOptionStyle.toStrings(includeTagsOption, includeTags),
    tagOptionStyle.toStrings(excludeTagsOption, excludeTags),
  ))

object FrameworkDescriptor:
  def forBackend(backend: ScalaBackend): List[FrameworkDescriptor] =
    all.toList.filter(_.maker(backend).isDefined)

  def forName(name: String): FrameworkDescriptor =
    arrayFind(all, _.name == name)
      .getOrElse(throw IllegalArgumentException(s"Test framework descriptor for '$name' not found"))

  def forClass(clazz: Class[? <: FrameworkDescriptor]): FrameworkDescriptor =
    arrayFind(all, _.getClass.getName.startsWith(clazz.getName))
      .getOrElse(throw IllegalArgumentException(s"Test framework descriptor for '$clazz' not found"))

  // This is a `def` and not a `val` because of some initialization complications ;)
  private def all: Array[FrameworkDescriptor] = Array(
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

  def dependency(
    framework: FrameworkDescriptor,
    backend: ScalaBackend,
    scalaVersion: ScalaVersion,
    version: Option[Version]
  ): Dependency#WithVersion =
    val maker: DependencyMaker = framework
      .maker(backend)
      .getOrElse(throw GradleException(s"Test framework $framework does not support $backend."))
    maker
      .dependency(scalaVersion)
      .withVersion(version.getOrElse(maker.versionDefaultFor(scalaVersion)))
