package org.podval.tools.build

import org.gradle.api.GradleException
import org.podval.tools.test.framework
import org.podval.tools.util.Scala212Collections.arrayFind
import sbt.testing.{Fingerprint, Runner, Framework as FrameworkSBT}

// Based on sbt.TestFramework.
abstract class TestFramework(
  val name: String,
  val nameSbt: String, // Name as reported by the framework
  val className: String,
  val sharedPackages: List[String],
  val tagOptions: Option[TagOptions],
  val usesTestSelectorAsNested: Boolean,
  val additionalOptions: Array[String]
) derives CanEqual:
  final override def toString: String = name

  // Note: `scalaVersion` is needed only to accommodate AirSpec.
  def isBackendSupported(backend: Backend, scalaVersion: ScalaVersion): Boolean = isBackendSupported(backend)

  def isBackendSupported(backend: Backend): Boolean = true

  // Note: `backend` and `scalaLibrary` parameter is needed only to accommodate specs2 -
  // so if the need goes away, this can be simplified ;)
  def versionDefault(backend: Backend, scalaLibrary: ScalaLibrary): Option[Version] = None

  def dependency: JvmDependency

  final def withVersion(
    backend: Backend,
    scalaLibrary: ScalaLibrary,
    version: Option[Version]
  ): DependencyVersion =
    val dependencyForBackend: JvmDependency = dependency.forBackend(Some(backend))
    dependencyForBackend
      .withVersion(
        scalaLibrary,
        version
          .orElse(versionDefault(dependencyForBackend.backend, scalaLibrary))
          .getOrElse(dependencyForBackend.versionDefault)
      )

  final def loaded(frameworkSBT: FrameworkSBT) = TestFramework.Loaded(this, frameworkSBT)

  final def load: TestFramework.Loaded = tryLoad.getOrElse(throw GradleException(s"Failed to load test framework $this!"))

  final def tryLoad: Option[TestFramework.Loaded] =
    try Class
      .forName(className)
      .getDeclaredConstructor()
      .newInstance()
    match
      case frameworkSBT: FrameworkSBT => Some(loaded(frameworkSBT))
      case other => throw GradleException(s"${other.getClass.getName} is not an SBT framework!")
    catch
      case _: ClassNotFoundException => None

object TestFramework:
  final class Loaded(
    val framework: TestFramework,
    frameworkSBT: FrameworkSBT
  ):
    require(framework.nameSbt == nameSbt)

    def nameSbt: String = frameworkSBT.name

    def fingerprints: Array[Fingerprint] = frameworkSBT.fingerprints

    def runner(args: Array[String]): Runner = frameworkSBT.runner(
      args,
      Array.empty,
      frameworkSBT.getClass.getClassLoader
    )
  
  def forNameSbt(nameSbt: String): TestFramework = find(_.nameSbt == nameSbt, nameSbt)

  def find(p: TestFramework => Boolean, what: String): TestFramework =
    arrayFind(all, p).getOrElse(throw GradleException(s"Test framework for '$what' not found"))

  val all: Array[TestFramework] = Array(
    org.podval.tools.jvm.JUnit4Jvm,
    org.podval.tools.scalajs.JUnit4ScalaJS,
    org.podval.tools.scalanative.JUnit4ScalaNative,
    framework.AirSpec,
    framework.Hedgehog,
    framework.MUnit,
    framework.ScalaCheck,
    framework.Scalaprops,
    framework.ScalaTest,
    framework.Specs2,
    framework.UTest,
    framework.WeaverTest,
    framework.ZioTest
  )
