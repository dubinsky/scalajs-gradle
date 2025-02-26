package org.podval.tools.test.framework

import org.podval.tools.build.{Dependency, ScalaPlatform, Version}

// Note: based on sbt.TestFramework from org.scala-sbt.testing
// TODO introduce optional backend-dependent dependency for the underlying framework.
abstract class FrameworkDescriptor(
  val name: String,
  val displayName: String,
  override val group: String,
  override val artifact: String,
  override val versionDefault: Version,
  val className: String,
  val sharedPackages: List[String]
) extends Dependency.Maker[ScalaPlatform] derives CanEqual:
  final def isSupported(platform: ScalaPlatform): Boolean =
    if platform.backend.isJS
    then isScalaJSSupported
    else isJvmSupported
  
  protected def isJvmSupported: Boolean = true

  // Note: if isScalaJSSupported, dependency must be a Scala one.
  protected def isScalaJSSupported: Boolean = true

  def args(
    includeTags: Set[String],
    excludeTags: Set[String]
  ): Seq[String]
  
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

  def apply(name: String): FrameworkDescriptor = all
    .find(_.name == name)
    .getOrElse(throw IllegalArgumentException(s"Test framework descriptor for '$name' not found"))

  def listOption(name: String, values: Set[String]): Seq[String] =
    if values.isEmpty
    then Seq.empty
    else
      val valuesStr: String = values.mkString(",")
      Seq(s"$name=$valuesStr")

  def listOptionNoEq(name: String, values: Set[String]): Seq[String] =
    if values.isEmpty
    then Seq.empty
    else Seq(name, values.mkString(","))

  def listOfOptions(name: String, values: Set[String]): Seq[String] =
    values.toIndexedSeq.flatMap((value: String) => Seq(name, value))
    