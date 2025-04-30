package org.podval.tools.scalajsplugin.scalajs

import org.gradle.api.GradleException
import org.podval.tools.node.TaskWithNode
import org.podval.tools.scalajs.{ModuleKind, ScalaJSCommon}
import org.podval.tools.scalajsplugin.nonjvm.BackendTask

trait ScalaJSTask extends BackendTask[ScalaJSLinkTask] with TaskWithNode:
  final override protected def backend: String = "Scala.js"

  protected final def scalaJSCommon: ScalaJSCommon = ScalaJSCommon(
    jsDirectory = linkTask.getJSDirectory,
    reportBinFile = linkTask.getReportBinFile,
    moduleKind = ModuleKind(linkTask.getModuleKind),
    logSource = s"ScalaJS $getName",
    logLifecycle = (message: String) => getLogger.lifecycle(message),
    abort = (message: String) => GradleException(message)
  )
