package org.podval.tools.scalajs.js

import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.podval.tools.build.{DependencyRequirement, ScalaDependency, ScalaLibrary, Version}

object ScalaJSDependencies:
  val configurationName: String = "scalajs"

  private val group: String = "org.scala-js"
  val versionDefault: Version = Version("1.18.2")

  // Note: no Scala 3 flavours exists
  object Library       extends ScalaDependency.Scala2(group, "scalajs-library")
  private object Compiler      extends ScalaDependency.Scala2(group, "scalajs-compiler", isScalaVersionFull = true)
  private object Linker        extends ScalaDependency.Scala2(group, "scalajs-linker")
  private object TestBridge    extends ScalaDependency.Scala2(group, "scalajs-test-bridge")
  private object TestAdapter   extends ScalaDependency.Scala2(group, "scalajs-sbt-test-adapter")
  private object TestInterface extends ScalaDependency.Scala2(group, "scalajs-test-interface")

  private object JSDomNodeJS extends ScalaDependency.Scala2(group, "scalajs-env-jsdom-nodejs"):
    val versionDefault: Version = Version("1.1.0")

  private object DomSJS:
    private val artifact: String = "scalajs-dom"
    val versionDefault: Version = Version("2.8.0")

    object Scala2 extends ScalaDependency.Scala2(group, artifact, isScalaJS = true)
    object Scala3 extends ScalaDependency.Scala3(group, artifact, isScalaJS = true)

  def forPlugin(
    pluginScalaLibrary: ScalaLibrary,
    scalaJSVersion: Version
  ): Seq[DependencyRequirement] =
    Seq(
      ScalaDependency.Requirement(
        findable = Linker,
        version = scalaJSVersion,
        scalaLibrary = pluginScalaLibrary,
        reason = "because it is needed for linking the ScalaJS code",
        configurationName = configurationName
      ),
      ScalaDependency.Requirement(
        findable = JSDomNodeJS,
        version = JSDomNodeJS.versionDefault,
        scalaLibrary = pluginScalaLibrary,
        reason = "because it is needed for running/testing with DOM man manipulations",
        configurationName = configurationName
      ),
//      ScalaDependency.Requirement(
//        findable = TestInterface,
//        version = scalaJSVersion,
//        scalaLibrary = pluginScalaLibrary,
//        reason =
//          """Zio Test on Scala.js seems to use `scalajs-test-interface`,
//            |although TestAdapter, confusingly, brings in `test-interface`
//            | - and so do most of the test frameworks (except for ScalaTest);
//            |even with this I get no test events from ZIO Test on Scala.js though...
//            |""".stripMargin,
//        configurationName = configurationName
//      ),
      ScalaDependency.Requirement(
        findable = TestAdapter,
        version = scalaJSVersion,
        scalaLibrary = pluginScalaLibrary,
        reason = "because it is needed for running the tests on Node",
        configurationName = configurationName
      )
    )

  def forProject(
    projectScalaLibrary: ScalaLibrary,
    scalaJSVersion: Version
  ): Seq[DependencyRequirement] =
    // only for Scala 3
    (if !projectScalaLibrary.isScala3 then Seq.empty else Seq(
      ScalaDependency.Requirement(
        findable = ScalaLibrary.Scala3SJS,
        version = ScalaLibrary.Scala3.getScalaVersion(projectScalaLibrary),
        scalaLibrary = projectScalaLibrary,
        reason = "because it is needed for linking of the ScalaJS code",
        configurationName = JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME
      )
    )) ++
    // only for Scala 2
    (if !projectScalaLibrary.isScala2 then Seq.empty else Seq(
      ScalaDependency.Requirement(
        findable = Compiler,
        version = scalaJSVersion,
        scalaLibrary = projectScalaLibrary,
        reason = "because it is needed for compiling of the ScalaJS code on Scala 2",
        configurationName = ScalaBasePlugin.SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME
      )
    )) ++ Seq(
      ScalaDependency.Requirement(
        findable = Library,
        version = scalaJSVersion,
        scalaLibrary = projectScalaLibrary,
        reason = "because it is needed for compiling of the ScalaJS code",
        configurationName = JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME
      ),
      ScalaDependency.Requirement(
        findable = if projectScalaLibrary.isScala3 then DomSJS.Scala3 else DomSJS.Scala2,
        version = DomSJS.versionDefault,
        scalaLibrary = projectScalaLibrary,
        reason = "because it is needed for DOM manipulations",
        configurationName = JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME
      ),
      ScalaDependency.Requirement(
        findable = TestBridge,
        version = scalaJSVersion,
        scalaLibrary = projectScalaLibrary,
        reason = "because it is needed for testing of the ScalaJS code",
        isVersionExact = true,
        configurationName = JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME
      )
    )
