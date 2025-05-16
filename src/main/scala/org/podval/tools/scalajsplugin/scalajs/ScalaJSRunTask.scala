package org.podval.tools.scalajsplugin.scalajs

import org.podval.tools.node.Node
import org.podval.tools.scalajs.ScalaJSRun
import org.podval.tools.scalajsplugin.nonjvm.NonJvmRunTask

trait ScalaJSRunTask extends NonJvmRunTask[ScalaJSLinkTask] with ScalaJSTask:
  final protected def scalaJSRun: ScalaJSRun = ScalaJSRun(
    jsDirectory = linkTask.getJSDirectory,
    reportBinFile = linkTask.getReportBinFile,
    moduleKind = linkTask.moduleKind,
    abort = abort
  )
