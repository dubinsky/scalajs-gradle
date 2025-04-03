package org.podval.tools.scalajsplugin

import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.{Plugin, Project}
import org.gradle.api.plugins.scala.ScalaPlugin
import org.podval.tools.build.{GradleClassPath, ScalaBackend, ScalaLibrary, ScalaPlatform}
import org.podval.tools.scalajsplugin.gradle.ScalaBasePlugin
import org.podval.tools.scalajsplugin.jvm.JvmDelegate
import org.podval.tools.scalajsplugin.scalajs.ScalaJSDelegate
import org.slf4j.{Logger, LoggerFactory}
import javax.inject.Inject

final class ScalaJSPlugin @Inject(
  jvmPluginServices: JvmPluginServices,
) extends Plugin[Project]:
  import ScalaJSPlugin.{logger, Mode}

  override def apply(project: Project): Unit =
    project.getPluginManager.apply(classOf[ScalaPlugin])

    val mode: Mode = ScalaJSPlugin.getMode(project)
    
    // Source sets, configurations and tasks!
    mode match
      case Mode.JVM =>
      case Mode.JS =>
      case Mode.Mixed =>
        ScalaBasePlugin(
          project = project,
          jvmPluginServices = jvmPluginServices,
          isCreate = false,
          sourceRoot = ScalaJSPlugin.jvmSourceRoot,
          sharedSourceRoot = ScalaJSPlugin.sharedSourceRoot,
          gradleNames = GradleNames.jvm
        )
          .apply()
        
        // TODO Scala.js side!

    val delegates: Seq[BackendDelegate] = mode match
      case Mode.JVM => Seq(
        JvmDelegate(project, GradleNames.jvm)
      )
      case Mode.JS => Seq(
        ScalaJSDelegate(project, GradleNames.jvm)
      )
      case Mode.Mixed => Seq(
        JvmDelegate(project, GradleNames.jvm),
        // TODO ScalaJSDelegate(project, GradleNames.js)
      )

    for delegate: BackendDelegate <- delegates do
      // Set up everything.
      val testTaskMaker: TestTaskMaker[?] = delegate.setUpProject()
      
      // Add test tasks.
      if mode == Mode.Mixed && delegate.isInstanceOf[ScalaJSDelegate]
      then testTaskMaker.register(project, "testJS")
      else testTaskMaker.replace (project, "test"  )
    
    project.afterEvaluate: (project: Project) =>
      val pluginScalaPlatform: ScalaPlatform =
        ScalaLibrary.getFromClasspath(GradleClassPath.collect(this)).toPlatform(ScalaBackend.Jvm)

      // Configure everything and add dependencies to configurations.
      val addToClassPath: Seq[AddToClassPath] = delegates.map(_.afterEvaluate(pluginScalaPlatform))

      // Expand classpath.
      addToClassPath.foreach(_.add(project))
      addToClassPath.foreach(_.verify(project))

private object ScalaJSPlugin:
  private enum Mode derives CanEqual:
    case JVM, JS, Mixed

  private val logger: Logger = LoggerFactory.getLogger(ScalaJSPlugin.getClass)

  private val disabledProperty: String = "org.podval.tools.scalajs.disabled"
  private val maiflaiProperty : String = "com.github.maiflai.gradle-scalatest.mode"

  private val sharedSourceRoot: String = "shared"
  private val jvmSourceRoot: String = "jvm"
  private val jsSourceRoot: String = "js"

  private def getMode(project: Project) =
    val isJSDisabled: Boolean =
      Option(project.findProperty(maiflaiProperty )).isDefined ||
      Option(project.findProperty(disabledProperty)).exists(_.toString.toBoolean)

    val sourceRootPresent: Boolean =
      project.file(jvmSourceRoot).exists ||
      project.file(jsSourceRoot).exists

    if isJSDisabled
    then
      logger.info("ScalaJSPlugin: running in JVM mode; Scala.js is disabled.")
      Mode.JVM
    else if !sourceRootPresent
    then
      logger.info("ScalaJSPlugin: running in Scala.js mode.")
      Mode.JS
    else
      if project.file("src").exists then
        logger.warn("ScalaJSPlugin: Both 'src' and platform-specific source roots are present! Ignoring 'src'.")
        
      logger.info("ScalaJSPlugin: running in mixed JVM/Scala.js mode.")
      Mode.Mixed
