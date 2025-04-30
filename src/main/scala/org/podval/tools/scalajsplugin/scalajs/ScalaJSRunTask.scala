package org.podval.tools.scalajsplugin.scalajs

import org.podval.tools.node.Node
import org.podval.tools.scalajs.ScalaJSRunCommon
import org.podval.tools.scalajsplugin.nonjvm.BackendRunTask

trait ScalaJSRunTask extends BackendRunTask[ScalaJSLinkTask] with ScalaJSTask:
  final protected def scalaJSRunCommon: ScalaJSRunCommon =
    val node: Node = linkTask.node
    ScalaJSRunCommon(
      scalaJSCommon,
      nodePath = node.installation.node.getAbsolutePath,
      nodeEnvironment = node.nodeEnv.toMap
    )
