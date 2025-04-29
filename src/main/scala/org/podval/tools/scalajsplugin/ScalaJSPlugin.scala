package org.podval.tools.scalajsplugin

import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.{Plugin, Project}
import org.gradle.api.plugins.scala.ScalaPlugin
import org.podval.tools.build.{GradleClassPath, ScalaBackendKind, ScalaLibrary, ScalaPlatform}
import org.podval.tools.scalajsplugin.jvm.JvmDelegate
import org.slf4j.{Logger, LoggerFactory}
import javax.inject.Inject

final class ScalaJSPlugin @Inject(
  jvmPluginServices: JvmPluginServices
) extends Plugin[Project]:
  override def apply(project: Project): Unit =
    project.getPluginManager.apply(classOf[ScalaPlugin])

    val delegates: Seq[BackendDelegate] = ScalaJSPlugin.getDelegates(project)

    // Source sets, configurations, and tasks!
    delegates.foreach(_.setUpProjectAndTestTask(jvmPluginServices))
    
    project.afterEvaluate: (project: Project) =>
      val pluginScalaPlatform: ScalaPlatform =
        ScalaLibrary.getFromClasspath(GradleClassPath.collect(this)).toPlatform(ScalaBackendKind.JVM)

      // Configure everything and add dependencies to configurations.
      val addToClassPath: Seq[AddToClassPath] = delegates.map(_.afterEvaluate(pluginScalaPlatform))

      // Expand classpath.
      addToClassPath.foreach(_.add(project))
      addToClassPath.foreach(_.verify(project))

object ScalaJSPlugin:
  private val logger: Logger = LoggerFactory.getLogger(ScalaJSPlugin.getClass)

  val modeProperty: String = "org.podval.tools.scalajs.mode"
  val mixedModeName: String = "Mixed"
  val maiflaiProperty: String = "com.github.maiflai.gradle-scalatest.mode"

  private def getDelegates(project: Project): Seq[BackendDelegate] =
    Option(project.findProperty(modeProperty)).map(_.toString) match
      case Some(name) =>
        if name == mixedModeName then None else BackendDelegateKind
          .all
          .find(_.name == name)
          .orElse(throw IllegalArgumentException(s"Unknown mode '$name'"))

      case None =>
        val isSourceRootPresent: Boolean = BackendDelegateKind
          .all
          .map(_.sourceRoot)
          .exists(sourceRoot => project.file(sourceRoot).exists)

        if isSourceRootPresent then None else Some(
          if Option(project.findProperty(maiflaiProperty)).isDefined
          then BackendDelegateKind.JVM
          else BackendDelegateKind.JS
        )
    match
      case None =>
        logger.info(s"ScalaJSPlugin: running in mixed mode.")
        // TODO BackendDelegateMaker.all.map(_.mk(project, isModeMixed = true))
        Seq(JvmDelegate(project, isModeMixed = true))
      case Some(kind) =>
        logger.info(s"ScalaJSPlugin: running in ${kind.name} mode.")
        Seq(kind.mk(project, isModeMixed = false))
