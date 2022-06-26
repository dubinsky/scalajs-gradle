package org.podval.tools.scalajs

import org.gradle.api.{DefaultTask, Project}
import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.gradle.api.tasks.TaskAction
import org.opentorah.build.Gradle.*
import org.podval.tools.scalajs.dependencies.GradleUtil
import java.lang.reflect.Method
import java.net.{URL, URLClassLoader}
import scala.jdk.CollectionConverters.*
import GradleUtil.*

abstract class ScalaJSTask extends DefaultTask:
  setGroup("build")
  setDescription(s"$flavour ScalaJS${stage.description}")

  protected def flavour: String

  protected def stage: Stage

  // Note: If dynamically-loaded classes are mentioned in the Task and Extension subclasses,
  // Gradle decorating code breaks at the plugin load time. That is why such code is in a separate class (Actions).
  @TaskAction final def execute(): Unit =
    ScalaJSTask.addFromConfiguration(this, ScalaBasePlugin.ZINC_CONFIGURATION_NAME   )
    ScalaJSTask.addFromConfiguration(this, ScalaJSPlugin  .SCALAJS_CONFIGURATION_NAME)
    doExecute(Actions(this))

  protected def doExecute(actions: Actions): Unit

object ScalaJSTask:
  trait FastOpt:
    self: ScalaJSTask =>

    final override protected def stage: Stage = Stage.FastOpt

  trait FullOpt:
    self: ScalaJSTask =>

    final override protected def stage: Stage = Stage.FullOpt

  trait FromExtension:
    self: ScalaJSTask =>

    final override protected def stage: Stage = getProject.getExtension(classOf[Extension]).stage

  private val addUrlMethod: Method = classOf[URLClassLoader].getDeclaredMethod("addURL", classOf[URL])
  addUrlMethod.setAccessible(true)

  private def addFromConfiguration(task: ScalaJSTask, configurationName: String): Unit =
    val classLoader: URLClassLoader = task.getClass.getClassLoader.asInstanceOf[URLClassLoader]
    task.getProject.getConfiguration(configurationName)
      .asScala
      .map(_.toURI.toURL)
      .foreach((url: URL) => addUrlMethod.invoke(classLoader, url))
