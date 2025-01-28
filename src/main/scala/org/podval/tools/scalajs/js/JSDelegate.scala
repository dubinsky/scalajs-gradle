package org.podval.tools.scalajs.js

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.scala.ScalaCompile
import org.podval.tools.build.{Gradle, GradleClassPath, ScalaLibrary, Version}
import org.podval.tools.node.NodeExtension
import org.podval.tools.scalajs.ScalaJSPlugin
import scala.jdk.CollectionConverters.{IterableHasAsScala, ListHasAsScala, SeqHasAsJava, SetHasAsScala}

class JSDelegate extends ScalaJSPlugin.Delegate:
  override def beforeEvaluate(project: Project): Unit =
    val nodeExtension: NodeExtension = NodeExtension.addTo(project)
    nodeExtension.getModules.convention(List("jsdom").asJava)
  
    val scalaJSConfiguration: Configuration = project.getConfigurations.create(ScalaJSDependencies.configurationName)
    scalaJSConfiguration.setVisible(false)
    scalaJSConfiguration.setCanBeConsumed(false)
    scalaJSConfiguration.setDescription("ScalaJS dependencies used by the ScalaJS plugin.")
  
    val linkMain: LinkTask.Main = project.getTasks.register("link"    , classOf[LinkTask.Main]).get()
    val run     : RunTask .Main = project.getTasks.register("run"     , classOf[RunTask .Main]).get()
    run.dependsOn (linkMain)
    val linkTest: LinkTask.Test = project.getTasks.register("linkTest", classOf[LinkTask.Test]).get()
    val test    : RunTask .Test = project.getTasks.replace ("test"    , classOf[RunTask .Test])
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
    
    ScalaJSDependencies.forPlugin(
      pluginScalaLibrary,
      scalaJSVersion
    ).foreach(_.applyToConfiguration(project))

    ScalaJSDependencies.forProject(
      projectScalaLibrary,
      scalaJSVersion
    ).foreach(_.applyToConfiguration(project))

    // Needed to access ScalaJS linking functionality in LinkTask.
    // Dynamically-loaded classes can only be loaded after they are added to the classpath,
    // or Gradle decorating code breaks at the plugin load time for the Task subclasses.
    // That is why dynamically-loaded classes are mentioned indirectly, only in the ScalaJS class.
    // TODO instead, add configuration itself to whatever configuration lists dependencies available to the plugin... "classpath"?
    GradleClassPath.addTo(this, Gradle.getConfiguration(project, ScalaJSDependencies.configurationName).asScala)

    if projectScalaLibrary.isScala3 then
      configureScalaCompileForScalaJs(project, SourceSet.MAIN_SOURCE_SET_NAME)
      configureScalaCompileForScalaJs(project, SourceSet.TEST_SOURCE_SET_NAME)

    // Now that whatever needs to be on the classpath already is, configure `LinkTask.runtimeClassPath` for all `LinkTask`s.
    project.getTasks.asScala.foreach {
      case linkTask: LinkTask =>
        linkTask.getRuntimeClassPath.setFrom(Gradle.getSourceSet(project, linkTask.sourceSetName).getRuntimeClasspath)
      case _ =>
    }

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
