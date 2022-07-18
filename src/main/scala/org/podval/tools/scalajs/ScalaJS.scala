package org.podval.tools.scalajs

import org.gradle.api.artifacts.Configuration
import org.opentorah.build.{Configurations, DependencyRequirement, Scala2Dependency, Scala3Dependency, ScalaLibrary}

object ScalaJS:
  val group: String = "org.scala-js"
  val versionDefault: String = "1.10.1"

  // Note: no Scala 3 flavours exists
  object Library     extends Scala2Dependency(group = group, nameBase = "scalajs-library")
  object TestBridge  extends Scala2Dependency(group = group, nameBase = "scalajs-test-bridge")
  object Linker      extends Scala2Dependency(group = group, nameBase = "scalajs-linker")
  object TestAdapter extends Scala2Dependency(group = group, nameBase = "scalajs-sbt-test-adapter")
  object Compiler    extends Scala2Dependency(group = group, nameBase = "scalajs-compiler", isScala2versionFull = true)

  object JSDomNodeJS extends Scala2Dependency(group = group, nameBase = "scalajs-env-jsdom-nodejs"):
    val versionDefault: String = "1.1.0"

  object DomSJS:
    val nameBase: String = "scalajs-dom_sjs1"
    val versionDefault: String = "2.2.0"

    object Scala2 extends Scala2Dependency(group = group, nameBase = nameBase)
    object Scala3 extends Scala3Dependency(group = group, nameBase = nameBase)

  val configurationName: String = "scalajs"
  private val configurations: Configurations = Configurations.forName(configurationName)

  def getVersion(configuration: Configuration): String = Library.getFromConfiguration(configuration)
    .map(_.version)
    .getOrElse(versionDefault)
  
  def requirements(
    pluginScalaLibrary: ScalaLibrary,
    projectScalaLibrary: ScalaLibrary,
    scalaJSVersion: String
  ): Seq[DependencyRequirement] =

    // only for Scala 3
    (if !projectScalaLibrary.isScala3 then Seq.empty else Seq(
      DependencyRequirement(
        dependency = ScalaLibrary.Scala3SJS,
        version = projectScalaLibrary.scala3.get.version,
        scalaLibrary = projectScalaLibrary,
        reason = "because it is needed for linking of the ScalaJS code",
        configurations = Configurations.implementation
      )
    )) ++
    // only for Scala 2
    (if !projectScalaLibrary.isScala2 then Seq.empty else Seq(
      DependencyRequirement(
        dependency = Compiler,
        version = scalaJSVersion,
        scalaLibrary = projectScalaLibrary,
        reason = "because it is needed for compiling of the ScalaJS code on Scala 2",
        configurations = Configurations.scalaCompilerPlugins
      )
    )) ++ Seq(
      DependencyRequirement(
        dependency = Library,
        version = scalaJSVersion,
        scalaLibrary = projectScalaLibrary,
        reason = "because it is needed for compiling of the ScalaJS code",
        configurations = Configurations.implementation
      ),
      DependencyRequirement(
        dependency = if projectScalaLibrary.isScala3 then DomSJS.Scala3 else DomSJS.Scala2,
        version = DomSJS.versionDefault,
        scalaLibrary = projectScalaLibrary,
        reason = "because it is needed for DOM manipulations",
        configurations = Configurations.implementation
      ),
      DependencyRequirement(
        dependency = TestBridge,
        version = scalaJSVersion,
        scalaLibrary = projectScalaLibrary,
        reason = "because it is needed for testing of the ScalaJS code",
        isVersionExact = true,
        configurations = Configurations.testImplementation
      ),
      
      // for the plugin classPath
      
      DependencyRequirement(
        dependency = Linker,
        version = scalaJSVersion,
        scalaLibrary = pluginScalaLibrary,
        reason = "because it is needed for linking the ScalaJS code",
        configurations = configurations
      ),
      DependencyRequirement(
        dependency = JSDomNodeJS,
        version = JSDomNodeJS.versionDefault,
        scalaLibrary = pluginScalaLibrary,
        reason = "because it is needed for running/testing with DOM man manipulations",
        configurations = configurations
      ),
      DependencyRequirement(
        dependency = TestAdapter,
        version = scalaJSVersion,
        scalaLibrary = pluginScalaLibrary,
        reason = "because it is needed for running the tests on Node",
        configurations = configurations
      )
    )
