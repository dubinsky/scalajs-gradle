package org.podval.tools.scalajs

import org.gradle.api.{Plugin, Project}
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.SourceSet
import org.opentorah.build.Gradle.*
import org.podval.tools.scalajs.dependencies.GradleUtil.*
import org.podval.tools.scalajs.dependencies.{ConfigurationNames, DependencyRequirement, GradleUtil, ScalaLibrary}
import scala.jdk.CollectionConverters.*

final class ScalaJSPlugin extends Plugin[Project]:
  override def apply(project: Project): Unit =
    project.getPluginManager.apply(classOf[org.gradle.api.plugins.scala.ScalaPlugin])

    project.getExtensions.create("scalajs", classOf[Extension], project)

    val configuration: Configuration = project.getConfigurations.create(ScalaJSPlugin.SCALAJS_CONFIGURATION_NAME)
    configuration.setVisible(false)
    configuration.setCanBeConsumed(false)
    configuration.setDescription("ScalaJS dependencies used by the ScalaJS plugin.")

    project.getTasks.create("sjsLinkFastOpt"    , classOf[LinkTask.Main.FastOpt      ])
    project.getTasks.create("sjsLinkFullOpt"    , classOf[LinkTask.Main.FullOpt      ])
    project.getTasks.create("sjsLink"           , classOf[LinkTask.Main.FromExtension])

    project.getTasks.create("sjsLinkTestFastOpt", classOf[LinkTask.Test.FastOpt      ])
    project.getTasks.create("sjsLinkTestFullOpt", classOf[LinkTask.Test.FullOpt      ])
    project.getTasks.create("sjsLinkTest"       , classOf[LinkTask.Test.FromExtension])

    project.getTasks.create("sjsRunFastOpt"     , classOf[RunTask      .FastOpt      ])
    project.getTasks.create("sjsRunFullOpt"     , classOf[RunTask      .FullOpt      ])
    project.getTasks.create("sjsRun"            , classOf[RunTask      .FromExtension])

    project.getTasks.create("sjsTestFastOpt"    , classOf[TestTask     .FastOpt      ])
    project.getTasks.create("sjsTestFullOpt"    , classOf[TestTask     .FullOpt      ])
    project.getTasks.create("sjsTest"           , classOf[TestTask     .FromExtension])

    project.afterEvaluate { (project: Project) =>
      configureProject(project)
    }

  private def configureProject(project: Project): Unit =
    val scalaLibrary: ScalaLibrary = ScalaLibrary.getFromConfiguration(project)

    val scalaJSVersion: String = ScalaJS.Library
      .getFromConfiguration(ConfigurationNames.implementation, project)
      .map(_.version)
      .getOrElse(ScalaJS.versionDefault)

    val requirements: Seq[DependencyRequirement] =
      // Note: only for Scala 3
      (if !scalaLibrary.isScala3 then Seq.empty else Seq(
        DependencyRequirement(
          dependency = ScalaLibrary.Scala3SJS,
          version = scalaLibrary.scala3.get.version,
          scalaLibrary = scalaLibrary,
          reason = "because it is needed for linking of the ScalaJS code"
        )
      )) ++
      // Note: only for Scala 2
      (if !scalaLibrary.isScala2 then Seq.empty else Seq(
        DependencyRequirement(
          dependency = ScalaJS.Compiler,
          version = scalaJSVersion,
          scalaLibrary = scalaLibrary,
          reason = "because it is needed for compiling of the ScalaJS code on Scala 2",
          configurationNames = ConfigurationNames.scalaCompilerPlugins
        )
      )) ++ Seq(
        DependencyRequirement(
          dependency = ScalaJS.Linker,
          version = scalaJSVersion,
          scalaLibrary = scalaLibrary,
          reason = "because it is needed for linking the ScalaJS code",
          configurationNames = ScalaJSPlugin.scalajsConfigurationNames
        ),

        DependencyRequirement(
          dependency = ScalaJS.Library,
          version = scalaJSVersion,
          scalaLibrary = scalaLibrary,
          reason = "because it is needed for compiling of the ScalaJS code"
        ),

        DependencyRequirement(
          dependency = if scalaLibrary.isScala3 then ScalaJS.DomSJS.Scala3 else ScalaJS.DomSJS.Scala2,
          version = ScalaJS.DomSJS.versionDefault,
          scalaLibrary = scalaLibrary,
          reason = "because it is needed for DOM manipulations"
        ),

        DependencyRequirement(
          dependency = ScalaJS.JSDomNodeJS,
          version = ScalaJS.JSDomNodeJS.versionDefault,
          scalaLibrary = scalaLibrary,
          reason = "because it is needed for running/testing with DOM man manipulations",
          configurationNames = ScalaJSPlugin.scalajsConfigurationNames
        ),

        DependencyRequirement(
          dependency = ScalaJS.TestBridge,
          version = scalaJSVersion,
          scalaLibrary = scalaLibrary,
          reason = "because it is needed for testing of the ScalaJS code",
          isVersionExact = true,
          configurationNames = ConfigurationNames.testImplementation
        ),

        DependencyRequirement(
          dependency = ScalaJS.TestAdapter,
          version = scalaJSVersion,
          scalaLibrary = scalaLibrary,
          reason = "because it is needed for running the tests on Node",
          configurationNames = ScalaJSPlugin.scalajsConfigurationNames
        )
      )

    requirements.foreach(_.applyToConfiguration(project))

    if scalaLibrary.isScala3 then
      configureScalaCompile(SourceSet.MAIN_SOURCE_SET_NAME, project)
      configureScalaCompile(SourceSet.TEST_SOURCE_SET_NAME, project)

    scalaLibrary.verifyFromClasspath(project)

    requirements.foreach(_.applyToClasspath(project))

  private def configureScalaCompile(
    sourceSetName: String,
    project: Project
  ): Unit =
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

object ScalaJSPlugin:
  val SCALAJS_CONFIGURATION_NAME: String = "scalajs"

  val scalajsConfigurationNames: ConfigurationNames = ConfigurationNames(
    toAdd = SCALAJS_CONFIGURATION_NAME,
    toCheck = SCALAJS_CONFIGURATION_NAME
  )
