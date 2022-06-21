package org.podval.tools.scalajs

import org.gradle.api.{Plugin, Project}
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.gradle.api.tasks.scala.ScalaCompile
import org.gradle.api.tasks.SourceSet
import org.opentorah.build.Gradle.*
import org.opentorah.util.Strings
import org.podval.tools.scalajs.dependencies.{DependencyRequirement, DependencyVersion, GradleUtil, ScalaLibrary}
import scala.jdk.CollectionConverters.*

// TODO test on a Scala 2 project: it seems that the scalajs-compiler plugin needs to be given as a path, not just dependency notation...
final class ScalaJSPlugin extends Plugin[Project]:
  override def apply(project: Project): Unit =
    project.getPluginManager.apply(classOf[org.gradle.api.plugins.scala.ScalaPlugin])

    project.getExtensions.create("scalajs", classOf[Extension])

    project.getTasks.create("sjsLinkFastOpt"    , classOf[LinkTask.Main.FastOpt  ])
    project.getTasks.create("sjsLinkFullOpt"    , classOf[LinkTask.Main.FullOpt  ])
    project.getTasks.create("sjsLink"           , classOf[LinkTask.Main.Extension])

    project.getTasks.create("sjsLinkTestFastOpt", classOf[LinkTask.Test.FastOpt  ])
    project.getTasks.create("sjsLinkTestFullOpt", classOf[LinkTask.Test.FullOpt  ])
    project.getTasks.create("sjsLinkTest"       , classOf[LinkTask.Test.Extension])

    project.getTasks.create("sjsRunFastOpt"     , classOf[RunTask      .FastOpt  ])
    project.getTasks.create("sjsRunFullOpt"     , classOf[RunTask      .FullOpt  ])
    project.getTasks.create("sjsRun"            , classOf[RunTask      .Extension])

    project.getTasks.create("sjsTestFastOpt"    , classOf[TestTask     .FastOpt  ])
    project.getTasks.create("sjsTestFullOpt"    , classOf[TestTask     .FullOpt  ])
    project.getTasks.create("sjsTest"           , classOf[TestTask     .Extension])

    project.afterEvaluate { (project: Project) =>
      configureProject(project)
    }

  private def configureProject(project: Project): Unit =
    val scalaLibrary: ScalaLibrary = ScalaLibrary.getFromConfiguration(project)
    val scala2versionMinor: String = scalaLibrary.scala2versionMinor

    val scalaJSVersion: String = ScalaJS.Library
      .getFromConfiguration(GradleUtil.getConfiguration(project, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME))
      .map(_.version)
      .getOrElse(ScalaJS.versionDefault)

    val requirements: Seq[DependencyRequirement] = Seq(
      DependencyRequirement(
        dependency = if scalaLibrary.isScala3 then ScalaLibrary.SJS.Scala3 else ScalaLibrary.SJS.Scala2,
        version = scalaLibrary.version,
        scala2versionMinor = scala2versionMinor,
        reason = "because it is needed for linking of the ScalaJS code"
      ),

      DependencyRequirement(
        dependency = ScalaJS.Library,
        version = scalaJSVersion,
        scala2versionMinor = scala2versionMinor,
        reason = "because it is needed for compiling of the ScalaJS code"
      ),

      DependencyRequirement(
        dependency = if scalaLibrary.isScala3 then ScalaJS.DomSJS.Scala3 else ScalaJS.DomSJS.Scala2,
        version = ScalaJS.DomSJS.versionDefault,
        scala2versionMinor = scala2versionMinor,
        reason = "because it is needed for DOM manipulations"
      ),

      DependencyRequirement(
        dependency = ScalaJS.TestBridge,
        version = scalaJSVersion,
        scala2versionMinor = scala2versionMinor,
        reason = "because it is needed for testing of the ScalaJS code",
        isTest = true,
        isVersionExact = true
      )
    )

    requirements.foreach(_.applyToConfiguration(project))

    if !scalaLibrary.isScala3 then
      DependencyRequirement(
        dependency = ScalaJS.Compiler,
        version = scalaJSVersion,
        scala2versionMinor = scala2versionMinor,
        reason = "because it is needed for compiling of the ScalaJS code on Scala 2",
        configurationName = Some(ScalaBasePlugin.SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME)
      )
        .applyToConfiguration(project)

    configureScalaCompile(SourceSet.MAIN_SOURCE_SET_NAME, scalaLibrary, project)
    configureScalaCompile(SourceSet.TEST_SOURCE_SET_NAME, scalaLibrary, project)

    val scalaLibraryFromClasspath: ScalaLibrary = ScalaLibrary.getFromClasspath(project)
    require(scalaLibraryFromClasspath.isScala3 == scalaLibrary.isScala3, "Scala 3 presence changed")
    if scalaLibrary.isScala3
    then require(scalaLibraryFromClasspath.scala3.get.version == scalaLibrary.scala3.get.version, "Scala 3 version changed")
    else require(scalaLibraryFromClasspath.scala2.get.version == scalaLibrary.scala2.get.version, "Scala 2 version changed")

    requirements.foreach(_.applyToClasspath(project))

  private def configureScalaCompile(
    sourceSetName: String,
    scalaLibrary: ScalaLibrary,
    project: Project
  ): Unit =
    def info(message: String): Unit =
      project.getLogger.info(s"scalaCompileOptions.additionalParameters of the $sourceSetName ScalaCompile task: $message", null, null, null)

    val scalaCompile: ScalaCompile = GradleUtil.getScalaCompile(project, project.getSourceSet(sourceSetName))
    val parameters: List[String] = scalaCompile.getScalaCompileOptions.getAdditionalParameters.asScala.toList
    def setParameters(parameters: List[String]): Unit = scalaCompile.getScalaCompileOptions.setAdditionalParameters(parameters.asJava)

    if scalaLibrary.isScala3 then
      if !parameters.contains("-scalajs") then
        info(s"Adding '-scalajs'")
        setParameters(parameters :+ "-scalajs")
    else parameters
        .find(_.startsWith("-Xplugin:"))
        .fold(Some(parameters))((xPlugin: String) =>
          if xPlugin.contains(ScalaJS.Compiler.nameBase) then None else Some(parameters.filterNot(_ == xPlugin))
        )
        .foreach { (newParameters: List[String]) =>
          val xPluginValue: String =
            project.getConfigurations.getByName(ScalaBasePlugin.SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME).getAsPath
          info(s"Adding '-Xplugin:$xPluginValue'")
          setParameters(newParameters :+ s"-Xplugin:$xPluginValue")
        }
