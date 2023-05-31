package org.podval.tools.scalajs

import org.gradle.api.artifacts.Configuration
import org.opentorah.build.{Configurations, DependencyRequirement, Scala2Dependency, Scala3Dependency, 
  ScalaDependencyRequirement, ScalaLibrary}

object ScalaJSDependencies:
  val configurationName: String = "scalajs"
  private val scalaJS: Configurations = Configurations.forName(configurationName)

  private val group: String = "org.scala-js"
  private val versionDefault: String = "1.13.1"

  // Note: no Scala 3 flavours exists
  private object Library     extends Scala2Dependency(group, "scalajs-library")
  private object TestBridge  extends Scala2Dependency(group, "scalajs-test-bridge")
  private object Linker      extends Scala2Dependency(group, "scalajs-linker")
  private object TestAdapter extends Scala2Dependency(group, "scalajs-sbt-test-adapter")
  private object Compiler    extends Scala2Dependency(group, "scalajs-compiler", isScala2versionFull = true)

  private object JSDomNodeJS extends Scala2Dependency(group, "scalajs-env-jsdom-nodejs"):
    val versionDefault: String = "1.1.0"

  private object DomSJS:
    private val artifact: String = "scalajs-dom_sjs1"
    val versionDefault: String = "2.6.0"

    object Scala2 extends Scala2Dependency(group, artifact)
    object Scala3 extends Scala3Dependency(group, artifact)

  def dependencyRequirements(
    pluginScalaLibrary: ScalaLibrary,
    projectScalaLibrary: ScalaLibrary,
    implementationConfiguration: Configuration
  ): Seq[DependencyRequirement] =
    
    val scalaJSVersion: String = Library.getFromConfiguration(implementationConfiguration)
      .map(_.version.version)
      .getOrElse(versionDefault)

    val forPluginClassPath: Seq[DependencyRequirement] =
      Seq(
        ScalaDependencyRequirement(
          dependency = Linker,
          version = scalaJSVersion,
          scalaLibrary = pluginScalaLibrary,
          reason = "because it is needed for linking the ScalaJS code",
          configurations = scalaJS
        ),
        ScalaDependencyRequirement(
          dependency = JSDomNodeJS,
          version = JSDomNodeJS.versionDefault,
          scalaLibrary = pluginScalaLibrary,
          reason = "because it is needed for running/testing with DOM man manipulations",
          configurations = scalaJS
        ),
        ScalaDependencyRequirement(
          dependency = TestAdapter,
          version = scalaJSVersion,
          scalaLibrary = pluginScalaLibrary,
          reason = "because it is needed for running the tests on Node",
          configurations = scalaJS
        )
      )

    val forProjectClassPath: Seq[DependencyRequirement] =
      // only for Scala 3
      (if !projectScalaLibrary.isScala3 then Seq.empty else Seq(
        ScalaDependencyRequirement(
          dependency = ScalaLibrary.Scala3SJS,
          version = projectScalaLibrary.scala3.get.version.version,
          scalaLibrary = projectScalaLibrary,
          reason = "because it is needed for linking of the ScalaJS code",
          configurations = Configurations.implementation
        )
      )) ++
      // only for Scala 2
      (if !projectScalaLibrary.isScala2 then Seq.empty else Seq(
        ScalaDependencyRequirement(
          dependency = Compiler,
          version = scalaJSVersion,
          scalaLibrary = projectScalaLibrary,
          reason = "because it is needed for compiling of the ScalaJS code on Scala 2",
          configurations = Configurations.scalaCompilerPlugins
        )
      )) ++ Seq(
        ScalaDependencyRequirement(
          dependency = Library,
          version = scalaJSVersion,
          scalaLibrary = projectScalaLibrary,
          reason = "because it is needed for compiling of the ScalaJS code",
          configurations = Configurations.implementation
        ),
        ScalaDependencyRequirement(
          dependency = if projectScalaLibrary.isScala3 then DomSJS.Scala3 else DomSJS.Scala2,
          version = DomSJS.versionDefault,
          scalaLibrary = projectScalaLibrary,
          reason = "because it is needed for DOM manipulations",
          configurations = Configurations.implementation
        ),
        ScalaDependencyRequirement(
          dependency = TestBridge,
          version = scalaJSVersion,
          scalaLibrary = projectScalaLibrary,
          reason = "because it is needed for testing of the ScalaJS code",
          isVersionExact = true,
          configurations = Configurations.testImplementation
        )
      )

    forPluginClassPath ++ forProjectClassPath
