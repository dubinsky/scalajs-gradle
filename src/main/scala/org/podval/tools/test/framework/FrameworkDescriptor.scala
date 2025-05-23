package org.podval.tools.test.framework

import org.podval.tools.build.jvm.JvmBackend
import org.podval.tools.build.scalajs.ScalaJSBackend
import org.podval.tools.build.scalanative.ScalaNativeBackend
import org.podval.tools.build.{Dependency, ScalaBackend, ScalaDependency, ScalaPlatform, Version}
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
  final val usesTestSelectorAsNestedTestSelector: Boolean = false,
  final val versionDefaultScala2: Option[Version] = None,
  final val forJVM: ForBackend = ForBackend(),
  final val forJS: ForBackend = ForBackend(),
  final val forNative: ForBackend = ForBackend()
) extends Dependency.Maker[ScalaPlatform] derives CanEqual:
  if forJS.isSupported then require(this.isInstanceOf[ScalaDependency.Maker])

  final override def description: String = displayName

  final def forBackend(backend: ScalaBackend): ForBackend = backend match
    case JvmBackend         => forJVM
    case ScalaJSBackend     => forJS
    case ScalaNativeBackend => forNative

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

  def forBackend(backend: ScalaBackend): List[FrameworkDescriptor] = all.toList.filter(_.forBackend(backend).isSupported)

  def forName(name: String): FrameworkDescriptor = arrayFind(all, _.name == name)
    .getOrElse(throw IllegalArgumentException(s"Test framework descriptor for '$name' not found"))
