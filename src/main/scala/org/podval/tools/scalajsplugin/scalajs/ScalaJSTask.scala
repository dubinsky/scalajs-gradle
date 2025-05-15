package org.podval.tools.scalajsplugin.scalajs

import org.gradle.api.GradleException
import org.podval.tools.node.TaskWithNode
import org.podval.tools.scalajs.{ModuleKind, ScalaJSCommon}
import org.podval.tools.scalajsplugin.nonjvm.NonJvmTask

trait ScalaJSTask extends NonJvmTask[ScalaJSLinkTask] with TaskWithNode:
  protected final def scalaJSCommon: ScalaJSCommon = ScalaJSCommon(
    jsDirectory = linkTask.getJSDirectory,
    reportBinFile = linkTask.getReportBinFile,
    moduleKind = ModuleKind(linkTask.getModuleKind),
    logSource = s"Scala.js $getName",
    abort = (message: String) => GradleException(message)
  )
