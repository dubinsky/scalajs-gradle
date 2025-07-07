package org.podval.tools.test.framework

import org.podval.tools.build.{DependencyMaker, ScalaBackend, ScalaDependencyMaker, ScalaVersion, Version}
import org.podval.tools.jvm.JvmBackend
import org.podval.tools.scalajs.ScalaJSBackend
import org.podval.tools.scalanative.ScalaNativeBackend
import org.podval.tools.util.Scala212Collections.{arrayConcat, arrayFind}

// Based on sbt.TestFramework.
abstract class FrameworkDescriptor(
  final val name: String,
  final val displayName: String,
  final val group: String,
  final val artifact: String,
  final val className: String,
  final val sharedPackages: List[String],
  tagOptionStyle: OptionStyle = OptionStyle.NotSupported,
  includeTagsOption: String = "",
  excludeTagsOption: String = "",
  additionalOptions: Array[String] = Array.empty,
  final val usesTestSelectorAsNestedTestSelector: Boolean = false,
  final val versionDefaultScala2: Option[Version] = None
) derives CanEqual:

  // Note: this is not a parameter to avoid circular initialization with NonJvmBackend.junit4;
  // alternative is to delay it there: `junit4: => FrameworkDescriptor`...
  def versionDefault: Version

  protected abstract class Maker extends DependencyMaker:
    //    if forJS.isDefined then require(this.isInstanceOf[ScalaDependencyMaker])
    final override def group: String = FrameworkDescriptor.this.group
    final override def artifact: String = FrameworkDescriptor.this.artifact
    final override def versionDefault: Version = FrameworkDescriptor.this.versionDefault
    final override def description: String = displayName

  protected class ScalaMaker(override val scalaBackend: ScalaBackend) extends Maker with ScalaDependencyMaker

  def maker     (backend: ScalaBackend): Option[DependencyMaker] = Some(ScalaMaker(backend))
  def underlying(backend: ScalaBackend): Option[DependencyMaker] = None

  final def args(
    includeTags: Array[String],
    excludeTags: Array[String]
  ): Array[String] = arrayConcat(additionalOptions, arrayConcat(
    tagOptionStyle.toStrings(includeTagsOption, includeTags),
    tagOptionStyle.toStrings(excludeTagsOption, excludeTags),
  ))

object FrameworkDescriptor:
  def forBackend(backend: ScalaBackend): List[FrameworkDescriptor] = all.toList.filter(_.maker(backend).isDefined)

  def forName(name: String): FrameworkDescriptor = arrayFind(all, _.name == name)
    .getOrElse(throw IllegalArgumentException(s"Test framework descriptor for '$name' not found"))

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
