package org.podval.tools.scalaplugin.scalajs

import org.podval.tools.build.scalajs.{ScalaJSBuild, ScalaJSTestEnvironment}
import org.podval.tools.scalaplugin.nonjvm.NonJvmRunTask

abstract class ScalaJSTestTask extends NonJvmRunTask.Test[ScalaJSLinkTask] with ScalaJSRunTask:
  final override protected def linkTaskClass: Class[ScalaJSLinkTestTask] = classOf[ScalaJSLinkTestTask]
  
  final override protected def createTestEnvironment: ScalaJSTestEnvironment = ScalaJSBuild.createTestEnvironment(
    jsDirectory = linkTask.getJSDirectory,
    reportBinFile = linkTask.getReportBinFile,
    moduleKind = linkTask.moduleKind,
    node = linkTask.node,
    logSource = getName,
    abort = abort
  )
