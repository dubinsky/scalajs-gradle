package org.podval.tools.backendplugin.scalajs

import org.podval.tools.backend.scalajs.{ScalaJSBuild, ScalaJSTestEnvironment}
import org.podval.tools.backendplugin.nonjvm.NonJvmRunTask

abstract class ScalaJSTestTask extends NonJvmRunTask.Test[ScalaJSLinkTask] with ScalaJSRunTask:
  final override protected def linkTaskClass: Class[ScalaJSLinkTestTask] = classOf[ScalaJSLinkTestTask]
  
  final override protected def createTestEnvironment: ScalaJSTestEnvironment = ScalaJSBuild.createTestEnvironment(
    jsDirectory = linkTask.getJSDirectory,
    reportBinFile = linkTask.getReportBinFile,
    moduleKind = linkTask.moduleKind,
    node = linkTask.node,
    useWebAssembly = linkTask.experimentalUseWebAssembly,
    jsEnvKind = jsEnvKind,
    browserName = browserName,
    logSource = getName,
    abort = abort
  )
