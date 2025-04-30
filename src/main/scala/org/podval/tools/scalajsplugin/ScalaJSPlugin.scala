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
    
    delegates.foreach(_.apply(jvmPluginServices))
    
    project.afterEvaluate: (project: Project) =>
      val pluginScalaPlatform: ScalaPlatform =
        ScalaLibrary.getFromClasspath(GradleClassPath.collect(this)).toPlatform(ScalaBackendKind.JVM)

      // Configure everything and add dependencies to configurations.
      val addToClassPath: Seq[AddToClassPath] = delegates.flatMap(_.afterEvaluate(
        pluginScalaPlatform
      ))

      // Expand classpath.
      addToClassPath.foreach(_.add(project))
      addToClassPath.foreach(_.verify(project))

object ScalaJSPlugin:
  private val logger: Logger = LoggerFactory.getLogger(ScalaJSPlugin.getClass)

  val backendProperty: String = "org.podval.tools.scalajs.backend"

  private def getDelegates(project: Project): Seq[BackendDelegate] =
    val isSourceRootPresent: Boolean = BackendDelegateKind
      .all
      .map(_.sourceRoot)
      .exists(sourceRoot => project.file(sourceRoot).exists)

    if isSourceRootPresent then
      logger.info(s"ScalaJSPlugin: running in multi-backend mode.")
      // TODO BackendDelegateMaker.all.map(_.mk(project, isModeMixed = true))
      Seq(JvmDelegate.mk(project, isModeMixed = true))
    else
      val kind: BackendDelegateKind = Option(project.findProperty(backendProperty)).map(_.toString) match
        case None => 
          JvmDelegate

        case Some(name) =>
          BackendDelegateKind
            .all
            .find(_.backendKind.name == name)
            .getOrElse(throw IllegalArgumentException(s"Unknown mode '$name'"))

      logger.info(s"ScalaJSPlugin: running on ${kind.backendKind.displayName} mode.")
      Seq(kind.mk(project, isModeMixed = false))
