package org.podval.tools.scalajs

import org.gradle.api.{Plugin, Project}
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.SourceSet
import org.opentorah.build.{Configurations, DependencyRequirement, JavaDependency, ScalaLibrary, Version}
import org.opentorah.build.Gradle.*
import org.opentorah.node.{NodeExtension, TaskWithNode}
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

      val implementationConfiguration: Configuration = project.getConfiguration(Configurations.implementationConfiguration)
      val pluginScalaLibrary : ScalaLibrary = ScalaLibrary.getFromClasspath(collectClassPath(getClass.getClassLoader))
      val projectScalaLibrary: ScalaLibrary = ScalaLibrary.getFromConfiguration(implementationConfiguration)

      val requirements: Seq[DependencyRequirement] =
        Seq(
          JavaDependency.Requirement(
            dependency = JavaDependency(group = "org.scala-sbt", artifact = "test-interface"),
            version = Version("1.0"),
            reason =
              """
                |because some test frameworks (ScalaTest :)) do not bring it in in,
                |and it needs to be on the testImplementation classpath;
                |when using ScalaJS, org.scala-js:scalajs-sbt-test-adapter brings it into the scalajs configuration
                |""".stripMargin,
            configurations = Configurations.testImplementation
          )
        ) ++
        (if isScalaJSDisabled then Seq.empty else
          ScalaJSDependencies.dependencyRequirements(
            pluginScalaLibrary,
            projectScalaLibrary,
            implementationConfiguration
          )
        )

      DependencyRequirement.applyToProject(requirements, project)

      if !isScalaJSDisabled && projectScalaLibrary.isScala3 then
        ScalaJSPlugin.configureScalaCompile(project, SourceSet.MAIN_SOURCE_SET_NAME)
        ScalaJSPlugin.configureScalaCompile(project, SourceSet.TEST_SOURCE_SET_NAME)

      projectScalaLibrary.verify(
        ScalaLibrary.getFromClasspath(project.getConfiguration(Configurations.implementationClassPath).asScala)
      )
    )

object ScalaJSPlugin:
  private val disabledProperty: String = "org.podval.tools.scalajs.disabled"
  private val maiflaiProperty : String = "com.github.maiflai.gradle-scalatest.mode"

  private def isScalaJSDisabled(project: Project): Boolean =
    Option(project.findProperty(maiflaiProperty )).isDefined ||
    Option(project.findProperty(disabledProperty)).exists(_.toString.toBoolean)

  private def configureScalaCompile(project: Project, sourceSetName: String): Unit =
    val scalaCompile: ScalaCompile = project.getScalaCompile(sourceSetName)
    val parameters: List[String] = Option(scalaCompile.getScalaCompileOptions.getAdditionalParameters) // Note: nullable
      .map(_.asScala.toList)
      .getOrElse(List.empty)

    if !parameters.contains("-scalajs") then
      project.getLogger.info(s"scalaCompileOptions.additionalParameters of the $sourceSetName ScalaCompile task: adding '-scalajs'",
        null, null, null)
      scalaCompile
        .getScalaCompileOptions
        .setAdditionalParameters((parameters :+ "-scalajs").asJava)
