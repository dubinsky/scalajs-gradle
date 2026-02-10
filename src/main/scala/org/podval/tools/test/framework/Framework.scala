package org.podval.tools.test.framework

import org.gradle.api.GradleException
import org.podval.tools.build.{Backend, JvmDependency, ScalaLibrary, ScalaVersion, Version, WithVersion}
import org.podval.tools.platform.Scala212Collections.arrayFind
import sbt.testing.{Fingerprint, Runner, Framework as FrameworkSBT}

// Based on sbt.TestFramework.
abstract class Framework(
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
  ): WithVersion =
    val dependencyForBackend: JvmDependency = dependency.forBackend(Some(backend))
    dependencyForBackend
      .withVersion(
        scalaLibrary,
        version
          .orElse(versionDefault(dependencyForBackend.backend, scalaLibrary))
          .getOrElse(dependencyForBackend.versionDefault)
      )

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
    require(framework.nameSbt == nameSbt)

    def nameSbt: String = frameworkSBT.name

    def fingerprints: Array[Fingerprint] = frameworkSBT.fingerprints

    def runner(args: Array[String]): Runner = frameworkSBT.runner(
      args,
      Array.empty,
      frameworkSBT.getClass.getClassLoader
    )

  object Loaded:
    def apply(frameworkSBT: FrameworkSBT): Loaded = new Loaded(
      frameworkSBT = frameworkSBT,
      framework = forNameSbt(frameworkSBT.name)
    )

  def forNameSbt(nameSbt: String): Framework = find(_.nameSbt == nameSbt, nameSbt)

  def find(p: Framework => Boolean, what: String): Framework =
    arrayFind(all, p).getOrElse(throw GradleException(s"Test framework for '$what' not found"))

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
