package org.podval.tools.scalajs

import org.gradle.api.artifacts.Configuration
import org.podval.tools.build.{Configurations, DependencyRequirement, ScalaDependency, ScalaLibrary, Version}

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

  def dependencyRequirements(
    pluginScalaLibrary: ScalaLibrary,
    projectScalaLibrary: ScalaLibrary,
    scalaJSVersion: Version
  ): Seq[DependencyRequirement] =

    val forPluginClassPath: Seq[DependencyRequirement] =
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
        // with this on the classpath, I get:
        //Class sbt.testing.Status does not have member field 'sbt.testing.Status Success'
        //	at org.podval.tools.testing.worker.TestClassProcessor$.org$podval$tools$testing$worker$TestClassProcessor$$$toResultType(TestClassProcessor.scala:360)
//        ScalaDependency.Requirement(
//          findable = TestInterface,
//          version = scalaJSVersion,
//          scalaLibrary = pluginScalaLibrary,
//          reason =
//            """seems that Zio Test works in Scala.js only with `scalajs-test-interface`;
//              |this is before the TestAdapter, since that one brings in `test-interface`
//              |""".stripMargin,
//          configurationName = configurationName
//        ),
        ScalaDependency.Requirement(
          findable = TestAdapter,
          version = scalaJSVersion,
          scalaLibrary = pluginScalaLibrary,
          reason = "because it is needed for running the tests on Node",
          configurationName = configurationName
        )
      )

    val forProjectClassPath: Seq[DependencyRequirement] =
      // only for Scala 3
      (if !projectScalaLibrary.isScala3 then Seq.empty else Seq(
        ScalaDependency.Requirement(
          findable = ScalaLibrary.Scala3SJS,
          version = ScalaLibrary.Scala3.getScalaVersion(projectScalaLibrary),
          scalaLibrary = projectScalaLibrary,
          reason = "because it is needed for linking of the ScalaJS code",
          configurationName = Configurations.implementation
        )
      )) ++
      // only for Scala 2
      (if !projectScalaLibrary.isScala2 then Seq.empty else Seq(
        ScalaDependency.Requirement(
          findable = Compiler,
          version = scalaJSVersion,
          scalaLibrary = projectScalaLibrary,
          reason = "because it is needed for compiling of the ScalaJS code on Scala 2",
          configurationName = Configurations.scalaCompilerPlugins
        )
      )) ++ Seq(
        ScalaDependency.Requirement(
          findable = Library,
          version = scalaJSVersion,
          scalaLibrary = projectScalaLibrary,
          reason = "because it is needed for compiling of the ScalaJS code",
          configurationName = Configurations.implementation
        ),
        ScalaDependency.Requirement(
          findable = if projectScalaLibrary.isScala3 then DomSJS.Scala3 else DomSJS.Scala2,
          version = DomSJS.versionDefault,
          scalaLibrary = projectScalaLibrary,
          reason = "because it is needed for DOM manipulations",
          configurationName = Configurations.implementation
        ),
        ScalaDependency.Requirement(
          findable = TestBridge,
          version = scalaJSVersion,
          scalaLibrary = projectScalaLibrary,
          reason = "because it is needed for testing of the ScalaJS code",
          isVersionExact = true,
          configurationName = Configurations.testImplementation
        )
      )

    forPluginClassPath ++ forProjectClassPath
