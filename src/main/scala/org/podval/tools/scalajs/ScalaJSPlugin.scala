package org.podval.tools.scalajs

import org.gradle.api.{Plugin, Project}
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.SourceSet
import org.opentorah.build.{Configurations, DependencyRequirement, DependencyVersion, ScalaLibrary}
import org.opentorah.build.Gradle.*
import org.podval.tools.test.{Sbt, TestTaskScala}
import java.io.File
import scala.jdk.CollectionConverters.*

final class ScalaJSPlugin extends Plugin[Project]:
  override def apply(project: Project): Unit =
    val isScalaJSDisabled: Boolean = ScalaJSPlugin.isScalaJSDisabled(project)

    project.getPluginManager.apply(classOf[org.gradle.api.plugins.scala.ScalaPlugin])

    ScalaJSPlugin.createInternalConfiguraton(
      project,
      name = Sbt.configurationName,
      description = "sbt dependencies used by the ScalaJS plugin."
    )

    if isScalaJSDisabled then
      project.getTasks.replace("test", classOf[TestTaskScala])
    else
      ScalaJSPlugin.createInternalConfiguraton(
        project,
        name = ScalaJS.configurationName,
        description = "ScalaJS dependencies used by the ScalaJS plugin."
      )

      val link: LinkTask.Main     = project.getTasks.create ("link"    , classOf[LinkTask.Main])
      val run : RunTask           = project.getTasks.create ("run"     , classOf[RunTask      ])
      run .dependsOn(link    )

      val linkTest: LinkTask.Test = project.getTasks.create ("linkTest", classOf[LinkTask.Test])
      val test    : TestTask      = project.getTasks.replace("test"    , classOf[TestTask     ])
      test.dependsOn(linkTest)

    project.afterEvaluate((project: Project) =>
      val implementation: Configuration = project.getConfiguration(Configurations.implementationConfiguration)

      val pluginScalaLibrary : ScalaLibrary = ScalaLibrary.getFromClasspath(collectClassPath(getClass.getClassLoader))
      val projectScalaLibrary: ScalaLibrary = ScalaLibrary.getFromConfiguration(implementation)

      val sbtVersion: String = Sbt.ZincPersist
        .getFromClassPath(project.getConfiguration(ScalaBasePlugin.ZINC_CONFIGURATION_NAME).asScala)
        .get
        .version

      val requirements: Seq[DependencyRequirement] = ScalaJSPlugin.sbtPluginRequirements(
        pluginScalaLibrary = pluginScalaLibrary,
        sbtVersion = sbtVersion
      ) ++ (
        if isScalaJSDisabled then Seq.empty else
          val scalaJSVersion: String = ScalaJS.Library.getFromConfiguration(implementation)
            .map(_.version)
            .getOrElse(ScalaJS.versionDefault)

          ScalaJSPlugin.scalaJSPluginRequirements(
            pluginScalaLibrary = pluginScalaLibrary,
            scalaJSVersion = scalaJSVersion
          ) ++
          ScalaJSPlugin.scalaJSRequirements(
            projectScalaLibrary = projectScalaLibrary,
            scalaJSVersion = scalaJSVersion
          )
      )

      // TODO use DependencyRequirement.applyToProject(requirements, project) when it is released
      requirements.foreach(_.applyToConfiguration(project))
      requirements.foreach(_.applyToClassPath(project))

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

  private def createInternalConfiguraton(
    project: Project,
    name: String,
    description: String
  ): Configuration =
    val result: Configuration = project.getConfigurations.create(name)
    result.setVisible(false)
    result.setCanBeConsumed(false)
    result.setDescription(description)
    result

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

  // for the plugin classPath
  private def sbtPluginRequirements(
    pluginScalaLibrary: ScalaLibrary,
    sbtVersion: String
  ): Seq[DependencyRequirement] =
    val sbt: Configurations = Configurations.forName(Sbt.configurationName)

    Seq(
      DependencyRequirement(
        dependency = Sbt.ZincPersist,
        version = sbtVersion,
        scalaLibrary = pluginScalaLibrary,
        reason = "because it is needed for interfacing with sbt",
        configurations = sbt
      )
    )

  // for the plugin classPath
  private def scalaJSPluginRequirements(
    pluginScalaLibrary: ScalaLibrary,
    scalaJSVersion: String
  ): Seq[DependencyRequirement] =

    val scalaJS: Configurations = Configurations.forName(ScalaJS.configurationName)

    Seq(
      DependencyRequirement(
        dependency = ScalaJS.Linker,
        version = scalaJSVersion,
        scalaLibrary = pluginScalaLibrary,
        reason = "because it is needed for linking the ScalaJS code",
        configurations = scalaJS
      ),
      DependencyRequirement(
        dependency = ScalaJS.JSDomNodeJS,
        version = ScalaJS.JSDomNodeJS.versionDefault,
        scalaLibrary = pluginScalaLibrary,
        reason = "because it is needed for running/testing with DOM man manipulations",
        configurations = scalaJS
      ),
      DependencyRequirement(
        dependency = ScalaJS.TestAdapter,
        version = scalaJSVersion,
        scalaLibrary = pluginScalaLibrary,
        reason = "because it is needed for running the tests on Node",
        configurations = scalaJS
      )
    )

  // for the project classPaths
  private def scalaJSRequirements(
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
        dependency = ScalaJS.Compiler,
        version = scalaJSVersion,
        scalaLibrary = projectScalaLibrary,
        reason = "because it is needed for compiling of the ScalaJS code on Scala 2",
        configurations = Configurations.scalaCompilerPlugins
      )
    )) ++ Seq(
      DependencyRequirement(
        dependency = ScalaJS.Library,
        version = scalaJSVersion,
        scalaLibrary = projectScalaLibrary,
        reason = "because it is needed for compiling of the ScalaJS code",
        configurations = Configurations.implementation
      ),
      DependencyRequirement(
        dependency = if projectScalaLibrary.isScala3 then ScalaJS.DomSJS.Scala3 else ScalaJS.DomSJS.Scala2,
        version = ScalaJS.DomSJS.versionDefault,
        scalaLibrary = projectScalaLibrary,
        reason = "because it is needed for DOM manipulations",
        configurations = Configurations.implementation
      ),
      DependencyRequirement(
        dependency = ScalaJS.TestBridge,
        version = scalaJSVersion,
        scalaLibrary = projectScalaLibrary,
        reason = "because it is needed for testing of the ScalaJS code",
        isVersionExact = true,
        configurations = Configurations.testImplementation
      )
    )
