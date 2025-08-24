package org.podval.tools.test.framework

import org.gradle.api.GradleException
import org.podval.tools.build.Dependency
import org.podval.tools.platform.Scala212Collections.arrayFind
import sbt.testing.{Fingerprint, Runner, Framework as FrameworkSBT}

// Based on sbt.TestFramework.
trait Framework extends Dependency derives CanEqual:
  override def toString: String = description
  def name: String
  def className: String
  def sharedPackages: List[String] // TODO remove?
  def tagOptions: Option[TagOptions]
  def usesTestSelectorAsNested: Boolean
  def additionalOptions: Array[String] = Array.empty

  final def load: Framework.Loaded = tryLoad.getOrElse(throw GradleException(s"Failed to load test framework $this!"))

  final def tryLoad: Option[Framework.Loaded] =
    try Class
      .forName(className)
      .getDeclaredConstructor()
      .newInstance()
    match
      case frameworkSBT: FrameworkSBT => Some(new Framework.Loaded(this, frameworkSBT))
      case other => throw GradleException(s"${other.getClass.getName} is not an SBT framework!")
    catch
      case _: ClassNotFoundException => None

object Framework:
  final class Loaded(
    val framework: Framework,
    frameworkSBT: FrameworkSBT
  ):
    require(framework.name == frameworkSBT.name)

    def name: String = frameworkSBT.name

    def fingerprints: Array[Fingerprint] = frameworkSBT.fingerprints

    def runner(args: Array[String]): Runner = frameworkSBT.runner(
      args,
      Array.empty,
      frameworkSBT.getClass.getClassLoader
    )

  object Loaded:
    def apply(frameworkSBT: FrameworkSBT): Loaded = new Loaded(
      frameworkSBT = frameworkSBT,
      framework = forName(frameworkSBT.name)
    )

  val all: Array[Framework] = Array(
    JUnit4Jvm,
    JUnit4ScalaJS,
    JUnit4ScalaNative,
    AirSpec,
    Hedgehog,
    MUnit,
    ScalaCheck,
    Scalaprops,
    ScalaTest,
    Specs2,
    UTest,
    WeaverTest,
    ZioTest
  )

  def forName(name: String): Framework = find(_.name == name, name)

  def find(p: Framework => Boolean, what: String): Framework =
    arrayFind(all, p).getOrElse(throw GradleException(s"Test framework for '$what' not found"))
