package org.podval.tools.test.framework

import org.podval.tools.build.{Dependency, ScalaPlatform, Version}

// Note: based on sbt.TestFramework from org.scala-sbt.testing
// TODO introduce optional backend-dependent dependency for the underlying framework.
abstract class FrameworkDescriptor(
  final val name: String,
  final val displayName: String,
  final override val group: String,
  final override val artifact: String,
  final override val versionDefault: Version,
  final val className: String,
  final val sharedPackages: List[String],
  tagOptionStyle: OptionStyle,
  includeTagsOption: String,
  excludeTagsOption: String,
  isScala2Supported: Boolean = true,
  final val versionDefaultScala2: Option[Version] = None,
  final val isJvmSupported: Boolean = true,
  // Note: if isScalaJSSupported, dependency must be a Scala one.
  final val isScalaJSSupported: Boolean = true
) extends Dependency.Maker[ScalaPlatform] derives CanEqual:
  final def versionDefault(platform: ScalaPlatform): Version =
    if platform.version.isScala3
    then versionDefault
    else versionDefaultScala2.getOrElse(versionDefault)
  
  final def isSupported(platform: ScalaPlatform): Boolean = 
    (platform.version.isScala3 || isScala2Supported) && (
      if platform.backend.isJS
      then isScalaJSSupported
      else isJvmSupported
    )
  
  final def args(
    includeTags: Array[String],
    excludeTags: Array[String]
  ): Seq[String] =
    tagOptionStyle.toStrings(includeTagsOption, includeTags) ++
    tagOptionStyle.toStrings(excludeTagsOption, excludeTags)
  
  final def newInstance: AnyRef = Class.forName(className)
    .getDeclaredConstructor()
    .newInstance()

object FrameworkDescriptor:
  val all: List[FrameworkDescriptor] = List(
    ScalaTest,
    ScalaCheck,
    Specs2,
    JUnit4,
    JUnit4ScalaJS,
    JUnit5,
    MUnit,
    UTest,
    ZioTest
  )

  def jvmSupported    : List[FrameworkDescriptor] = all.filter(_.isJvmSupported    )
  def scalaJSSupported: List[FrameworkDescriptor] = all.filter(_.isScalaJSSupported)

  def forName(name: String): FrameworkDescriptor = all
    .find(_.name == name)
    .getOrElse(throw IllegalArgumentException(s"Test framework descriptor for '$name' not found"))
