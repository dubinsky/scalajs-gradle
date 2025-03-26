package org.podval.tools.scalajsplugin.scalajs

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.scala.ScalaCompile
import org.podval.tools.build.{DependencyRequirement, Gradle, ScalaModules, ScalaPlatform, ScalaVersion, Version}
import org.podval.tools.node.NodeExtension
import org.podval.tools.scalajs.ScalaJS
import org.podval.tools.scalajsplugin.BackendDelegate
import org.podval.tools.test.framework.JUnit4ScalaJS
import org.slf4j.{Logger, LoggerFactory}
import scala.jdk.CollectionConverters.{IterableHasAsScala, ListHasAsScala, SeqHasAsJava, SetHasAsScala}

final class ScalaJSDelegate(project: Project, isMixed: Boolean) extends BackendDelegate(project):
  override def setUpProject(): Unit =
    val nodeExtension: NodeExtension = NodeExtension.addTo(project)
    nodeExtension.getModules.convention(List("jsdom").asJava)

    Gradle.createConfiguration(project, ScalaJSDelegate.scalaJSConfigurationName, "ScalaJS dependencies used by the ScalaJS plugin.")
    Gradle.createConfiguration(project, ScalaJSDelegate.scalaJSCompilerPluginsConfigurationName, "ScalaJS Scala compiler plugins.")

    val linkMain: ScalaJSLinkMainTask = project.getTasks.register("link"    , classOf[ScalaJSLinkMainTask]).get()
    val run     : ScalaJSRunMainTask  = project.getTasks.register("run"     , classOf[ScalaJSRunMainTask ]).get()
    run.dependsOn (linkMain)
    val linkTest: ScalaJSLinkTestTask = project.getTasks.register("testLink", classOf[ScalaJSLinkTestTask]).get()
    val test    : ScalaJSTestTask     = project.getTasks.replace ("test"    , classOf[ScalaJSTestTask    ])
    test.dependsOn(linkTest)

  override def configurationToAddToClassPath: Option[String] = Some(ScalaJSDelegate.scalaJSConfigurationName)

  override def dependencyRequirements(
    pluginScalaPlatform: ScalaPlatform,
    projectScalaPlatform: ScalaPlatform
  ): Seq[DependencyRequirement] =
    val scalaJSVersion: Version = ScalaJS.Library
      .findInConfiguration(projectScalaPlatform, project, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME)
      .map(_.version)
      .getOrElse(ScalaJS.versionDefault)

    val isJUnit4Present: Boolean = JUnit4ScalaJS
      .findInConfiguration(projectScalaPlatform, project, JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME)
      .isDefined
    
    ScalaJSDelegate.forPlugin(
      pluginScalaPlatform,
      scalaJSVersion
    ) ++
    ScalaJSDelegate.forProject(
      projectScalaPlatform,
      scalaJSVersion,
      isJUnit4Present = isJUnit4Present
    )

  override def configureProject(projectScalaPlatform: ScalaPlatform): Unit =
    val isScala3: Boolean = projectScalaPlatform.version.isScala3
    ScalaJSDelegate.configureScalaCompile(project, SourceSet.MAIN_SOURCE_SET_NAME, isScala3)
    ScalaJSDelegate.configureScalaCompile(project, SourceSet.TEST_SOURCE_SET_NAME, isScala3)

    // TODO disable compileJava task for the Scala.js sourceSet - unless Scala.js compiler deals with Java classes?
    
    // Now that whatever needs to be on the classpath already is, configure `LinkTask.runtimeClassPath` for all `LinkTask`s.
    project.getTasks.asScala.foreach:
      case linkTask: ScalaJSLinkTask =>
        linkTask.getRuntimeClassPath.setFrom(Gradle.getSourceSet(project, linkTask.sourceSetName).getRuntimeClasspath)
      case _ =>

object ScalaJSDelegate:
  private val logger: Logger = LoggerFactory.getLogger(ScalaJSDelegate.getClass)

  final val sourceRoot: String = "js"

  private val scalaJSConfigurationName: String = "scalajs"
  private val scalaJSCompilerPluginsConfigurationName: String = "scalajsCompilerPlugins"

  private def forPlugin(
    pluginScalaPlatform: ScalaPlatform,
    scalaJSVersion: Version
  ): Seq[DependencyRequirement] = Seq(
//    ScalaModules.ParallelCollections.required(
//      platform = pluginScalaPlatform,
//      reason = "Scala.js linker (and possibly other Scala.js dependencies) uses it but does not bring it in somehow",
//      configurationName = scalaJSConfigurationName
//    ),
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
    ScalaJS.TestAdapter.required(
      platform = pluginScalaPlatform,
      version = scalaJSVersion,
      reason = "because it is needed for running the tests on Node",
      configurationName = scalaJSConfigurationName
    )
  )

  private def forProject(
    projectScalaPlatform: ScalaPlatform,
    scalaJSVersion: Version,
    isJUnit4Present: Boolean
  ): Seq[DependencyRequirement] =
    // only for Scala 2
    (if projectScalaPlatform.version.isScala3 then Seq.empty else Seq(
      ScalaJS.Compiler.required(
        platform = projectScalaPlatform,
        version = scalaJSVersion,
        reason = "because it is needed for compiling of the ScalaJS code on Scala 2",
        configurationName = scalaJSCompilerPluginsConfigurationName
      )
    )) ++
    // only for Scala 2 and only when JUnit4 is in use
    // (without JUnit on classpath this compiler plugin causes compiler errors)
    (if projectScalaPlatform.version.isScala3 || !isJUnit4Present then Seq.empty else Seq(
      ScalaJS.JUnitPlugin.required(
        platform = projectScalaPlatform,
        version = scalaJSVersion,
        reason = "because it is needed to generate bootstrappers for JUnit tests on Scala 2",
        configurationName = scalaJSCompilerPluginsConfigurationName
      )
    )) ++
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

  private def configureScalaCompile(
    project: Project,
    sourceSetName: String,
    isScala3: Boolean
  ): Unit =
    val scalaCompile: ScalaCompile = Gradle.getScalaCompile(project, sourceSetName)

    if isScala3 then
      val parameters: List[String] = Option(scalaCompile.getScalaCompileOptions.getAdditionalParameters) // nullable
        .map(_.asScala.toList)
        .getOrElse(List.empty)

      if !parameters.contains("-scalajs") then
        logger.info(s"scalaCompileOptions.additionalParameters of the $sourceSetName ScalaCompile task: adding '-scalajs'.")
        scalaCompile
          .getScalaCompileOptions
          .setAdditionalParameters((parameters :+ "-scalajs").asJava)
    else
      // There seems to be no need to add `"-Xplugin:" + plugin.getPath` parameters:
      // just adding plugins to the list is sufficient.
      val scalaJSCompilerPlugins: FileCollection = Gradle.getConfiguration(project, scalaJSCompilerPluginsConfigurationName)
      if scalaJSCompilerPlugins.asScala.nonEmpty then
        logger.info(s"scalaCompilerPlugins of the $sourceSetName ScalaCompile task: adding ${scalaJSCompilerPlugins.asScala}.")
        val plugins: FileCollection = Option(scalaCompile.getScalaCompilerPlugins)
          .map((existingPlugins: FileCollection) => existingPlugins.plus(scalaJSCompilerPlugins))
          .getOrElse(scalaJSCompilerPlugins)
        scalaCompile.setScalaCompilerPlugins(plugins)
