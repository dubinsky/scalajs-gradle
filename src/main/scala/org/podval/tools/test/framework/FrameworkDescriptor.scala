package org.podval.tools.test.framework

import org.podval.tools.build.{Dependency, ScalaDependency, ScalaPlatform, Version}
import org.podval.tools.util.Scala212Collections.{arrayConcat, arrayFind}

// Based on sbt.TestFramework.
abstract class FrameworkDescriptor(
  final val name: String,
  final val displayName: String,
  final override val group: String,
  final override val artifact: String,
  final override val versionDefault: Version,
  final val className: String,
  final val sharedPackages: List[String],
  tagOptionStyle: OptionStyle = OptionStyle.NotSupported,
  includeTagsOption: String = "",
  excludeTagsOption: String = "",
  additionalOptions: Array[String] = Array.empty,
  final val includesClassNameInTestName: Boolean = false,
  final val versionDefaultScala2: Option[Version] = None,
  final val isJvmSupported: Boolean = true,
  final val isScalaJSSupported: Boolean = true,
  final val jvmUnderlying: Option[Dependency.Maker[?]] = None,
  final val scalaJSUnderlying: Option[ScalaDependency.Maker] = None
) extends Dependency.Maker[ScalaPlatform] derives CanEqual:
  if isScalaJSSupported then require(this.isInstanceOf[ScalaDependency.Maker])

  final def versionDefault(platform: ScalaPlatform): Version =
    if platform.version.isScala3
    then versionDefault
    else versionDefaultScala2.getOrElse(versionDefault)
  
  final def isSupported(platform: ScalaPlatform): Boolean = 
      if platform.backend.isJS
      then isScalaJSSupported
      else isJvmSupported
  
  final def underlying(platform: ScalaPlatform): Option[Dependency.Maker[?]] =
    if platform.backend.isJS
    then scalaJSUnderlying
    else jvmUnderlying
    
  final def args(
    includeTags: Array[String],
    excludeTags: Array[String]
  ): Array[String] = arrayConcat(additionalOptions, arrayConcat(
    tagOptionStyle.toStrings(includeTagsOption, includeTags),
    tagOptionStyle.toStrings(excludeTagsOption, excludeTags),
  ))
  
  final def newInstance: AnyRef = Class.forName(className)
    .getDeclaredConstructor()
    .newInstance()

object FrameworkDescriptor:
  private def all: Array[FrameworkDescriptor] = Array(
    JUnit4,
    JUnit4ScalaJS,
    JUnit5,
    MUnit,
    ScalaTest,
    ScalaCheck,
    Specs2,
    UTest,
    ZioTest
  )

  def jvmSupported    : List[FrameworkDescriptor] = all.toList.filter(_.isJvmSupported    )
  def scalaJSSupported: List[FrameworkDescriptor] = all.toList.filter(_.isScalaJSSupported)

  def forName(name: String): FrameworkDescriptor = arrayFind(all, _.name == name)
    .getOrElse(throw IllegalArgumentException(s"Test framework descriptor for '$name' not found"))
