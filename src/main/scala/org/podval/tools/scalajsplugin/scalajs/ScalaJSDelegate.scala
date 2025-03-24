package org.podval.tools.scalajsplugin.scalajs

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.scala.ScalaCompile
import org.podval.tools.build.{DependencyRequirement, Gradle, GradleClassPath, ScalaModules,  ScalaPlatform, 
  ScalaVersion, Version}
import org.podval.tools.node.NodeExtension
import org.podval.tools.scalajs.ScalaJS
import org.podval.tools.scalajsplugin.BackendDelegate
import org.podval.tools.test.framework.JUnit4ScalaJS
import scala.jdk.CollectionConverters.{IterableHasAsScala, ListHasAsScala, SeqHasAsJava, SetHasAsScala}

class ScalaJSDelegate extends BackendDelegate:
  override def beforeEvaluate(project: Project): Unit =
    val nodeExtension: NodeExtension = NodeExtension.addTo(project)
    nodeExtension.getModules.convention(List("jsdom").asJava)

    val scalaJSConfiguration: Configuration = project.getConfigurations.create(ScalaJSDelegate.scalaJSConfigurationName)
    scalaJSConfiguration.setVisible(false)
    scalaJSConfiguration.setCanBeConsumed(false)
    scalaJSConfiguration.setDescription("ScalaJS dependencies used by the ScalaJS plugin.")

    val linkMain: ScalaJSLinkMainTask = project.getTasks.register("link"    , classOf[ScalaJSLinkMainTask]).get()
    val run     : ScalaJSRunMainTask  = project.getTasks.register("run"     , classOf[ScalaJSRunMainTask ]).get()
    run.dependsOn (linkMain)
    val linkTest: ScalaJSLinkTestTask = project.getTasks.register("testLink", classOf[ScalaJSLinkTestTask]).get()
    val test    : ScalaJSTestTask     = project.getTasks.replace ("test"    , classOf[ScalaJSTestTask    ])
    test.dependsOn(linkTest)

  override def configurationToAddToClassPath: Option[String] = Some(ScalaJSDelegate.scalaJSConfigurationName)

  override def configureProject(
    project: Project,
    projectScalaPlatform: ScalaPlatform
  ): Unit =
    if projectScalaPlatform.version.isScala3 then
      ScalaJSDelegate.configureScalaCompileForScalaJs(project, SourceSet.MAIN_SOURCE_SET_NAME)
      ScalaJSDelegate.configureScalaCompileForScalaJs(project, SourceSet.TEST_SOURCE_SET_NAME)
    
    // Now that whatever needs to be on the classpath already is, configure `LinkTask.runtimeClassPath` for all `LinkTask`s.
    project.getTasks.asScala.foreach:
      case linkTask: ScalaJSLinkTask =>
        linkTask.getRuntimeClassPath.setFrom(Gradle.getSourceSet(project, linkTask.sourceSetName).getRuntimeClasspath)
      case _ =>
  
  override def dependencyRequirements(
    project: Project,
    pluginScalaPlatform: ScalaPlatform,
    projectScalaPlatform: ScalaPlatform
  ): Seq[DependencyRequirement] =
    val scalaJSVersion: Version = ScalaJS.Library
      .findable(projectScalaPlatform)
      .findInConfiguration(Gradle.getConfiguration(project, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME))
      .map(_.version)
      .getOrElse(ScalaJS.versionDefault)

    val isJUnit4Present: Boolean = JUnit4ScalaJS
      .findable(projectScalaPlatform)
      .findInConfiguration(Gradle.getConfiguration(project, JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME))
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

object ScalaJSDelegate:
  private val scalaJSConfigurationName: String = "scalajs"

  private def configureScalaCompileForScalaJs(project: Project, sourceSetName: String): Unit =
    val scalaCompile: ScalaCompile = Gradle.getScalaCompile(project, sourceSetName)
    val parameters: List[String] = Option(scalaCompile.getScalaCompileOptions.getAdditionalParameters) // nullable
      .map(_.asScala.toList)
      .getOrElse(List.empty)

    if !parameters.contains("-scalajs") then
      project.getLogger.info(s"scalaCompileOptions.additionalParameters of the $sourceSetName ScalaCompile task: adding '-scalajs'",
        null, null, null)
      scalaCompile
        .getScalaCompileOptions
        .setAdditionalParameters((parameters :+ "-scalajs").asJava)
  
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
        configurationName = ScalaBasePlugin.SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME
      )
    )) ++
    // only for Scala 2 and only when JUnit4 is in use
    // (without JUnit on classpath this compiler plugin causes compiler errors)
    (if projectScalaPlatform.version.isScala3 || !isJUnit4Present then Seq.empty else Seq(
      ScalaJS.JUnitPlugin.required(
        platform = projectScalaPlatform,
        version = scalaJSVersion,
        reason = "because it is needed to generate bootstrappers for JUnit tests on Scala 2",
        configurationName = ScalaBasePlugin.SCALA_COMPILER_PLUGINS_CONFIGURATION_NAME
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
