package org.podval.tools.scalajs

import org.gradle.api.Project
import org.opentorah.build.{ConfigurationNames, DependencyRequirement, Scala2Dependency, Scala3Dependency, ScalaLibrary}

object ScalaJS:
  val group: String = "org.scala-js"
  val versionDefault: String = "1.10.1"

  // Note: no Scala 3 flavours exists
  object Library extends Scala2Dependency(group = group, nameBase = "scalajs-library"):
    def forVersions(
      scalaLibrary: ScalaLibrary,
      scalaJSVersion: String
    ): DependencyRequirement = DependencyRequirement(
      dependency = Library,
      version = scalaJSVersion,
      scalaLibrary = scalaLibrary,
      reason = "because it is needed for compiling of the ScalaJS code"
    )

  // Note: no Scala 3 flavours exists
  object TestBridge extends Scala2Dependency(group = group, nameBase = "scalajs-test-bridge"):
    def forVersions(
      scalaLibrary: ScalaLibrary,
      scalaJSVersion: String
    ): DependencyRequirement = DependencyRequirement(
      dependency = TestBridge,
      version = scalaJSVersion,
      scalaLibrary = scalaLibrary,
      reason = "because it is needed for testing of the ScalaJS code",
      isVersionExact = true,
      configurationNames = ConfigurationNames.testImplementation
    )

  // Note: no Scala 3 flavours exists
  object Linker extends Scala2Dependency(group = group, nameBase = "scalajs-linker"):
    def forVersions(
      scalaLibrary: ScalaLibrary,
      scalaJSVersion: String
    ): DependencyRequirement = DependencyRequirement(
      dependency = Linker,
      version = scalaJSVersion,
      scalaLibrary = scalaLibrary,
      reason = "because it is needed for linking the ScalaJS code",
      configurationNames = configurationNames
    )

  // Note: no Scala 3 flavours exists
  object TestAdapter extends Scala2Dependency(group = group, nameBase = "scalajs-sbt-test-adapter"):
    def forVersions(
      scalaLibrary: ScalaLibrary,
      scalaJSVersion: String
    ): DependencyRequirement = DependencyRequirement(
      dependency = TestAdapter,
      version = scalaJSVersion,
      scalaLibrary = scalaLibrary,
      reason = "because it is needed for running the tests on Node",
      configurationNames = configurationNames
    )

  // Note: no Scala 3 flavours exists
  // Note: only for Scala 2
  object Compiler extends Scala2Dependency(group = group, nameBase = "scalajs-compiler", isScala2versionFull = true):
    def forVersions(
      scalaLibrary: ScalaLibrary,
      scalaJSVersion: String
    ): Option[DependencyRequirement] = if !scalaLibrary.isScala2 then None else Some(
      DependencyRequirement(
        dependency = Compiler,
        version = scalaJSVersion,
        scalaLibrary = scalaLibrary,
        reason = "because it is needed for compiling of the ScalaJS code on Scala 2",
        configurationNames = ConfigurationNames.scalaCompilerPlugins
      )
    )

  object JSDomNodeJS extends Scala2Dependency(group = group, nameBase = "scalajs-env-jsdom-nodejs"):
    val versionDefault: String = "1.1.0"

    def forVersions(scalaLibrary: ScalaLibrary): DependencyRequirement = DependencyRequirement(
      dependency = JSDomNodeJS,
      version = JSDomNodeJS.versionDefault,
      scalaLibrary = scalaLibrary,
      reason = "because it is needed for running/testing with DOM man manipulations",
      configurationNames = configurationNames
    )

  object DomSJS:
    val nameBase: String = "scalajs-dom_sjs1"
    val versionDefault: String = "2.2.0"

    object Scala2 extends Scala2Dependency(group = group, nameBase = nameBase)
    object Scala3 extends Scala3Dependency(group = group, nameBase = nameBase)

    def forVersions(scalaLibrary: ScalaLibrary): DependencyRequirement = DependencyRequirement(
      dependency = if scalaLibrary.isScala3 then Scala3 else Scala2,
      version = DomSJS.versionDefault,
      scalaLibrary = scalaLibrary,
      reason = "because it is needed for DOM manipulations"
    )

  val configurationName: String = "scalajs"
  private val configurationNames: ConfigurationNames = ConfigurationNames(
    toAdd = configurationName,
    toCheck = configurationName
  )

  def forVersions(
    scalaLibrary: ScalaLibrary,
    project: Project
  ): Seq[DependencyRequirement] =

    val scalaJSVersion: String = Library
      .getFromConfiguration(ConfigurationNames.implementation, project)
      .map(_.version)
      .getOrElse(versionDefault)

    Compiler   .forVersions(scalaLibrary, scalaJSVersion).toSeq ++ Seq(
    Linker     .forVersions(scalaLibrary, scalaJSVersion),
    Library    .forVersions(scalaLibrary, scalaJSVersion),
    DomSJS     .forVersions(scalaLibrary),
    JSDomNodeJS.forVersions(scalaLibrary),
    TestBridge .forVersions(scalaLibrary, scalaJSVersion),
    TestAdapter.forVersions(scalaLibrary, scalaJSVersion))
