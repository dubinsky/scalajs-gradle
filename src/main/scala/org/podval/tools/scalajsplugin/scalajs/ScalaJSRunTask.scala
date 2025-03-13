package org.podval.tools.scalajsplugin.scalajs

import org.gradle.api.GradleException
import org.podval.tools.node.Node
import org.podval.tools.scalajs.ScalaJSRunCommon
import scala.jdk.CollectionConverters.SetHasAsScala

trait ScalaJSRunTask extends ScalaJSTask:
  protected def linkTaskClass: Class[? <: ScalaJSLinkTask]

  final override protected def linkTask: ScalaJSLinkTask = getDependsOn
    .asScala
    .find((candidate: AnyRef) => linkTaskClass.isAssignableFrom(candidate.getClass))
    .map(_.asInstanceOf[ScalaJSLinkTask])
    .getOrElse(throw GradleException(s"Task $getName must depend on a task of type ${linkTaskClass.getName}!"))

  final protected def scalaJSRunCommon: ScalaJSRunCommon =
    val node: Node = linkTask.node
    ScalaJSRunCommon(
      scalaJSCommon,
      nodePath = node.installation.node.getAbsolutePath,
      nodeEnvironment = node.nodeEnv.toMap
    )
