package org.podval.tools.test.framework

import org.podval.tools.build.jvm.JvmBackend
import org.podval.tools.build.scalajs.ScalaJSBackend
import org.podval.tools.build.scalanative.ScalaNativeBackend
import org.podval.tools.build.{Dependency, ScalaBackend, ScalaDependency, ScalaVersion, Version}
import org.podval.tools.util.Scala212Collections.{arrayConcat, arrayFind}

// Based on sbt.TestFramework.
abstract class FrameworkDescriptor(
  final val name: String,
  final val displayName: String,
  final val group: String,
  final val artifact: String,
  final val versionDefault: Version,
  final val className: String,
  final val sharedPackages: List[String],
  tagOptionStyle: OptionStyle = OptionStyle.NotSupported,
  includeTagsOption: String = "",
  excludeTagsOption: String = "",
  additionalOptions: Array[String] = Array.empty,
  final val usesTestSelectorAsNestedTestSelector: Boolean = false,
  final val versionDefaultScala2: Option[Version] = None
) derives CanEqual:
  
  def forJVM   : Option[ForBackend] = Some(ForBackend(ScalaMaker(JvmBackend        )))
  def forJS    : Option[ForBackend] = Some(ForBackend(ScalaMaker(ScalaJSBackend    )))
  def forNative: Option[ForBackend] = Some(ForBackend(ScalaMaker(ScalaNativeBackend)))
  
  final def forBackend(backend: ScalaBackend): Option[ForBackend] = backend match
    case JvmBackend         => forJVM
    case ScalaJSBackend     => forJS
    case ScalaNativeBackend => forNative

  protected abstract class Maker extends Dependency.Maker:
//    if forJS.isDefined then require(this.isInstanceOf[ScalaDependency.Maker])
    final override val group: String = FrameworkDescriptor.this.group
    final override val artifact: String = FrameworkDescriptor.this.artifact
    final override val versionDefault: Version = FrameworkDescriptor.this.versionDefault
    final override def description: String = displayName
  
  protected class ScalaMaker(override val scalaBackend: ScalaBackend) extends Maker with ScalaDependency.Maker
    
  final def args(
    includeTags: Array[String],
    excludeTags: Array[String]
  ): Array[String] = arrayConcat(additionalOptions, arrayConcat(
    tagOptionStyle.toStrings(includeTagsOption, includeTags),
    tagOptionStyle.toStrings(excludeTagsOption, excludeTags),
  ))

object FrameworkDescriptor:
  private val all: Array[FrameworkDescriptor] = Array(
    JUnit4,
    JUnit4ScalaJS,
    JUnit4ScalaNative,
    JUnit5,
    MUnit,
    ScalaTest,
    ScalaCheck,
    Specs2,
    UTest,
    ZioTest
  )

  def forBackend(backend: ScalaBackend): List[FrameworkDescriptor] = all.toList.filter(_.forBackend(backend).isDefined)

  def forName(name: String): FrameworkDescriptor = arrayFind(all, _.name == name)
    .getOrElse(throw IllegalArgumentException(s"Test framework descriptor for '$name' not found"))
