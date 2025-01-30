package org.podval.tools.scalajsplugin

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.scala.ScalaCompile
import org.podval.tools.build.*
import org.podval.tools.node.NodeExtension
import org.podval.tools.scalajs.ScalaJSDependencies
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
    pluginScalaLibrary : ScalaLibrary,
    projectScalaLibrary: ScalaLibrary
  ): Unit =
    val scalaJSVersion: Version = ScalaJSDependencies.Library
      .findInConfiguration(Gradle.getConfiguration(project, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME))
      .map(_.version)
      .getOrElse(ScalaJSDependencies.versionDefault)

    ScalaJSDelegate.forPlugin(
      pluginScalaLibrary,
      scalaJSVersion
    ).foreach(_.applyToConfiguration(project))

    ScalaJSDelegate.forProject(
      projectScalaLibrary,
      scalaJSVersion
    ).foreach(_.applyToConfiguration(project))

    // Needed to access ScalaJS linking functionality in LinkTask.
    // Dynamically-loaded classes can only be loaded after they are added to the classpath,
    // or Gradle decorating code breaks at the plugin load time for the Task subclasses.
    // That is why dynamically-loaded classes are mentioned indirectly, only in the ScalaJS class.
    // TODO instead, add configuration itself to whatever configuration lists dependencies available to the plugin... "classpath"?
    GradleClassPath.addTo(this, Gradle.getConfiguration(project, ScalaJSDelegate.scalaJSConfigurationName).asScala)

    if projectScalaLibrary.isScala3 then
      ScalaJSDelegate.configureScalaCompileForScalaJs(project, SourceSet.MAIN_SOURCE_SET_NAME)
      ScalaJSDelegate.configureScalaCompileForScalaJs(project, SourceSet.TEST_SOURCE_SET_NAME)

    // Now that whatever needs to be on the classpath already is, configure `LinkTask.runtimeClassPath` for all `LinkTask`s.
    project.getTasks.asScala.foreach {
      case linkTask: ScalaJSLinkTask =>
        linkTask.getRuntimeClassPath.setFrom(Gradle.getSourceSet(project, linkTask.sourceSetName).getRuntimeClasspath)
      case _ =>
    }

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
    pluginScalaLibrary: ScalaLibrary,
    scalaJSVersion: Version
  ): Seq[DependencyRequirement] = Seq(
    ScalaDependency.Requirement(
      findable = ScalaJSDependencies.Linker,
      version = scalaJSVersion,
      scalaLibrary = pluginScalaLibrary,
      reason = "because it is needed for linking the ScalaJS code",
      configurationName = scalaJSConfigurationName
    ),
    ScalaDependency.Requirement(
      findable = ScalaJSDependencies.JSDomNodeJS,
      version = ScalaJSDependencies.JSDomNodeJS.versionDefault,
      scalaLibrary = pluginScalaLibrary,
      reason = "because it is needed for running/testing with DOM man manipulations",
      configurationName = scalaJSConfigurationName
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
    //        scalaJSConfigurationName = scalaJSConfigurationName
    //      ),
    ScalaDependency.Requirement(
      findable = ScalaJSDependencies.TestAdapter,
      version = scalaJSVersion,
      scalaLibrary = pluginScalaLibrary,
      reason = "because it is needed for running the tests on Node",
      configurationName = scalaJSConfigurationName
    )
  )

  private def forProject(
    projectScalaLibrary: ScalaLibrary,
    scalaJSVersion: Version
  ): Seq[DependencyRequirement] =
    // only for Scala 3
    (if !projectScalaLibrary.isScala3 then Seq.empty else Seq(
      ScalaDependency.Requirement(
        findable = ScalaLibraryDependency.Scala3.ScalaJS,
        version = ScalaLibraryDependency.Scala3.scalaVersion(projectScalaLibrary),
        scalaLibrary = projectScalaLibrary,
        reason = "because it is needed for linking of the ScalaJS code",
        configurationName = JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME
      )
    )) ++
    // only for Scala 2
    (if !projectScalaLibrary.isScala2 then Seq.empty else Seq(
      ScalaDependency.Requirement(
        findable = ScalaJSDependencies.Compiler,
        version = scalaJSVersion,
        scalaLibrary = projectScalaLibrary,
        reason = "because it is needed for compiling of the ScalaJS code on Scala 2",
        configurationName = ScalaBasePlugin.SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME
      )
    )) ++ Seq(
    ScalaDependency.Requirement(
      findable = ScalaJSDependencies.Library,
      version = scalaJSVersion,
      scalaLibrary = projectScalaLibrary,
      reason = "because it is needed for compiling of the ScalaJS code",
      configurationName = JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME
    ),
    ScalaDependency.Requirement(
      findable = if projectScalaLibrary.isScala3 then ScalaJSDependencies.DomSJS.Scala3 else ScalaJSDependencies.DomSJS.Scala2,
      version = ScalaJSDependencies.DomSJS.versionDefault,
      scalaLibrary = projectScalaLibrary,
      reason = "because it is needed for DOM manipulations",
      configurationName = JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME
    ),
    ScalaDependency.Requirement(
      findable = ScalaJSDependencies.TestBridge,
      version = scalaJSVersion,
      scalaLibrary = projectScalaLibrary,
      reason = "because it is needed for testing of the ScalaJS code",
      isVersionExact = true,
      configurationName = JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME
    )
  )
