package org.podval.tools.scalajs

import org.gradle.api.{Plugin, Project}
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.SourceSet
import org.podval.tools.build.{Configurations, GradleClassPath, JavaDependency, ScalaLibrary, Version}
import org.podval.tools.build.Gradle.*
import org.podval.tools.node.NodeExtension
import org.podval.tools.testing.task.TestTaskScala
import scala.jdk.CollectionConverters.*

final class ScalaJSPlugin extends Plugin[Project]:
  override def apply(project: Project): Unit =
    project.getPluginManager.apply(classOf[org.gradle.api.plugins.scala.ScalaPlugin])

    val isScalaJSDisabled: Boolean = ScalaJSPlugin.isScalaJSDisabled(project)

    if isScalaJSDisabled then
      project.getTasks.replace("test", classOf[TestTaskScala])
    else
      NodeExtension.addTo(project)

      val scalaJS: Configuration = project.getConfigurations.create(ScalaJSDependencies.configurationName)
      scalaJS.setVisible(false)
      scalaJS.setCanBeConsumed(false)
      scalaJS.setDescription("ScalaJS dependencies used by the ScalaJS plugin.")

      val linkMain: LinkTask.Main = project.getTasks.register("link"    , classOf[LinkTask.Main]).get()
      val run     : RunTask .Main = project.getTasks.register("run"     , classOf[RunTask .Main]).get()
      run.dependsOn(linkMain)
      val linkTest: LinkTask.Test = project.getTasks.register("linkTest", classOf[LinkTask.Test]).get()
      val test    : RunTask .Test = project.getTasks.replace ("test"    , classOf[RunTask .Test])
      test.dependsOn(linkTest)

    project.afterEvaluate((project: Project) =>
      if !isScalaJSDisabled then NodeExtension.get(project).ensureNodeIsInstalled()

      val implementationConfiguration: Configuration = project.getConfiguration(Configurations.implementation)
      val pluginScalaLibrary : ScalaLibrary = ScalaLibrary.getFromClasspath(GradleClassPath.collect(this))
      val projectScalaLibrary: ScalaLibrary = ScalaLibrary.getFromConfiguration(implementationConfiguration)

      // AnalysisDetector, which runs during execution of TestTask, needs Zinc classes;
      // if I ever get rid of it, this classpath expansion goes away.
      GradleClassPath.addTo(this, project.getConfiguration(Configurations.zinc).asScala)
      
      if isScalaJSDisabled then Seq(
        JavaDependency.Requirement(
          dependency = JavaDependency(group = "org.scala-sbt", artifact = "test-interface"),
          version = Version("1.0"),
          reason =
            """
              |because some test frameworks (ScalaTest :)) do not bring it in in,
              |and it needs to be on the testImplementation classpath;
              |when using ScalaJS, org.scala-js:scalajs-sbt-test-adapter brings it into the scalajs configuration
              |""".stripMargin,
          configurationName = Configurations.testImplementation
        ).applyToConfiguration(project)
      ) else
        val scalaJSVersion: Version = ScalaJSDependencies.Library.findInConfiguration(implementationConfiguration)
          .map(_.version)
          .getOrElse(ScalaJSDependencies.versionDefault)

        // TODO do I need to add https://github.com/scala-js/scala-js/tree/main/test-interface too?

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
        GradleClassPath.addTo(this, project.getConfiguration(ScalaJSDependencies.configurationName).asScala)

        if projectScalaLibrary.isScala3 then
          ScalaJSPlugin.configureScalaCompile(project, SourceSet.MAIN_SOURCE_SET_NAME)
          ScalaJSPlugin.configureScalaCompile(project, SourceSet.TEST_SOURCE_SET_NAME)

      projectScalaLibrary.verify(
        ScalaLibrary.getFromClasspath(project.getConfiguration(Configurations.runtimeClassPath).asScala)
      )
    )

object ScalaJSPlugin:
  private val disabledProperty: String = "org.podval.tools.scalajs.disabled"
  private val maiflaiProperty : String = "com.github.maiflai.gradle-scalatest.mode"

  private def isScalaJSDisabled(project: Project): Boolean =
    Option(project.findProperty(maiflaiProperty )).isDefined ||
    Option(project.findProperty(disabledProperty)).exists(_.toString.toBoolean)

  private def configureScalaCompile(project: Project, sourceSetName: String): Unit =
    val scalaCompile: ScalaCompile = project.getScalaCompile(project.getSourceSet(sourceSetName))
    val parameters: List[String] = Option(scalaCompile.getScalaCompileOptions.getAdditionalParameters) // Note: nullable
      .map(_.asScala.toList)
      .getOrElse(List.empty)

    if !parameters.contains("-scalajs") then
      project.getLogger.info(s"scalaCompileOptions.additionalParameters of the $sourceSetName ScalaCompile task: adding '-scalajs'",
        null, null, null)
      scalaCompile
        .getScalaCompileOptions
        .setAdditionalParameters((parameters :+ "-scalajs").asJava)
