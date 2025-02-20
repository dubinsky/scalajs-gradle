package org.podval.tools.scalajsplugin

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.scala.ScalaCompile
import org.podval.tools.build.{DependencyRequirement, Gradle, GradleClassPath, ScalaPlatform, ScalaVersion, Version}
import org.podval.tools.node.NodeExtension
import org.podval.tools.scalajs.ScalaJS
import scala.jdk.CollectionConverters.{IterableHasAsScala, ListHasAsScala, SeqHasAsJava, SetHasAsScala}

class ScalaJSDelegate extends ScalaJSPlugin.Delegate:
  override def beforeEvaluate(project: Project): Unit =
    val nodeExtension: NodeExtension = NodeExtension.addTo(project)
    nodeExtension.getModules.convention(List("jsdom").asJava)

    val scalaJSConfiguration: Configuration = project.getConfigurations.create(ScalaJSDelegate.scalaJSConfigurationName)
    scalaJSConfiguration.setVisible(false)
    scalaJSConfiguration.setCanBeConsumed(false)
    scalaJSConfiguration.setDescription("ScalaJS dependencies used by the ScalaJS plugin.")

    val linkMain: ScalaJSLinkTask.Main = project.getTasks.register("link"    , classOf[ScalaJSLinkTask.Main]).get()
    val run     : ScalaJSRunTask .Main = project.getTasks.register("run"     , classOf[ScalaJSRunTask .Main]).get()
    run.dependsOn (linkMain)
    val linkTest: ScalaJSLinkTask.Test = project.getTasks.register("linkTest", classOf[ScalaJSLinkTask.Test]).get()
    val test    : ScalaJSRunTask .Test = project.getTasks.replace ("test"    , classOf[ScalaJSRunTask .Test])
    test.dependsOn(linkTest)

  override def afterEvaluate(
    project: Project,
    pluginScalaPlatform: ScalaPlatform,
    projectScalaPlatform: ScalaPlatform
  ): Unit =
    val scalaJSVersion: Version = ScalaJS.Library
      .findable(projectScalaPlatform)
      .findInConfiguration(Gradle.getConfiguration(project, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME))
      .map(_.version)
      .getOrElse(ScalaJS.versionDefault)

    ScalaJSDelegate.forPlugin(
      pluginScalaPlatform,
      scalaJSVersion
    ).foreach(_.applyToConfiguration(project))

    ScalaJSDelegate.forProject(
      projectScalaPlatform,
      scalaJSVersion
    ).foreach(_.applyToConfiguration(project))

    // Needed to access ScalaJS linking functionality in LinkTask.
    // Dynamically-loaded classes can only be loaded after they are added to the classpath,
    // or Gradle decorating code breaks at the plugin load time for the Task subclasses.
    // That is why dynamically-loaded classes are mentioned indirectly, only in the ScalaJS class.
    // TODO instead, add configuration itself to whatever configuration lists dependencies available to the plugin... "classpath"?
    GradleClassPath.addTo(this, Gradle.getConfiguration(project, ScalaJSDelegate.scalaJSConfigurationName).asScala)

    if projectScalaPlatform.version.isScala3 then
      ScalaJSDelegate.configureScalaCompileForScalaJs(project, SourceSet.MAIN_SOURCE_SET_NAME)
      ScalaJSDelegate.configureScalaCompileForScalaJs(project, SourceSet.TEST_SOURCE_SET_NAME)

    // Now that whatever needs to be on the classpath already is, configure `LinkTask.runtimeClassPath` for all `LinkTask`s.
    project.getTasks.asScala.foreach:
      case linkTask: ScalaJSLinkTask =>
        linkTask.getRuntimeClassPath.setFrom(Gradle.getSourceSet(project, linkTask.sourceSetName).getRuntimeClasspath)
      case _ =>

object ScalaJSDelegate:
  private def configureScalaCompileForScalaJs(project: Project, sourceSetName: String): Unit =
    val scalaCompile: ScalaCompile = Gradle.getScalaCompile(project, sourceSetName)
    val parameters: List[String] = Option(scalaCompile.getScalaCompileOptions.getAdditionalParameters) // Note: nullable
      .map(_.asScala.toList)
      .getOrElse(List.empty)

    if !parameters.contains("-scalajs") then
      project.getLogger.info(s"scalaCompileOptions.additionalParameters of the $sourceSetName ScalaCompile task: adding '-scalajs'",
        null, null, null)
      scalaCompile
        .getScalaCompileOptions
        .setAdditionalParameters((parameters :+ "-scalajs").asJava)

  private val scalaJSConfigurationName: String = "scalajs"

  private def forPlugin(
    pluginScalaPlatform: ScalaPlatform,
    scalaJSVersion: Version
  ): Seq[DependencyRequirement] = Seq(
    ScalaJS.Linker.required(
      platform = pluginScalaPlatform,
      version = scalaJSVersion,
      reason = "because it is needed for linking the ScalaJS code",
      configurationName = scalaJSConfigurationName
    ),
    ScalaJS.JSDomNodeJS.required(
      platform = pluginScalaPlatform,
      reason = "because it is needed for running/testing with DOM man manipulations",
      configurationName = scalaJSConfigurationName
    ),
    //      ScalaJSDependencies.TestInterface.require(
    //        scalaPlatform = pluginScalaPlatform,
    //        version = scalaJSVersion,
    //        reason =
    //          """Zio Test on Scala.js seems to use `scalajs-test-interface`,
    //            |although TestAdapter, confusingly, brings in `test-interface`
    //            | - and so do most of the test frameworks (except for ScalaTest);
    //            |even with this I get no test events from ZIO Test on Scala.js though...
    //            |""".stripMargin,
    //        scalaJSConfigurationName = scalaJSConfigurationName
    //      ),
    ScalaJS.TestAdapter.required(
      platform = pluginScalaPlatform,
      version = scalaJSVersion,
      reason = "because it is needed for running the tests on Node",
      configurationName = scalaJSConfigurationName
    )
  )

  private def forProject(
    projectScalaPlatform: ScalaPlatform,
    scalaJSVersion: Version
  ): Seq[DependencyRequirement] =
    // only for Scala 3
    (if !projectScalaPlatform.version.isScala3 then Seq.empty else Seq(
      // org.scala-lang:scala3-library_3:
      //   org.scala-lang:scala-library:2.13.x
      // org.scala-lang:scala3-library_sjs1_3
      //   org.scala-js:scalajs-javalib
      //   org.scala-js:scalajs-scalalib_2.13
      // org.scala-js:scalajs-library_2.13
      ScalaVersion.Scala3.ScalaLibraryJS.required(
        platform = projectScalaPlatform,
        version = projectScalaPlatform.scalaVersion,
        reason = "because it is needed for linking of the ScalaJS code",
        configurationName = JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME
      )
    )) ++
    // only for Scala 2
    (if projectScalaPlatform.version.isScala3 then Seq.empty else Seq(
      ScalaJS.Compiler.required(
        platform = projectScalaPlatform,
        version = scalaJSVersion,
        reason = "because it is needed for compiling of the ScalaJS code on Scala 2",
        configurationName = ScalaBasePlugin.SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME
      )
    )) ++ Seq(
      ScalaJS.Library.required(
        platform = projectScalaPlatform,
        version = scalaJSVersion,
        reason = "because it is needed for compiling of the ScalaJS code",
        configurationName = JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME
      ),
      ScalaJS.DomSJS.required(
        platform = projectScalaPlatform,
        reason = "because it is needed for DOM manipulations",
        configurationName = JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME
      ),
      // Dependencies:
      // org.scala-js:scalajs-test-bridge_2.13
      //   org.scala-js:scalajs-test-interface_2.13
      ScalaJS.TestBridge.required(
        platform = projectScalaPlatform,
        version = scalaJSVersion,
        reason = "because it is needed for testing of the ScalaJS code",
        isVersionExact = true,
        configurationName = JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME
      )
  )
