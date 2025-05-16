package org.podval.tools.scalajsplugin.scalajs

import org.podval.tools.build.ScalaBackendKind
import org.podval.tools.scalajs.ScalaJSTestEnvironment
import org.podval.tools.scalajsplugin.nonjvm.NonJvmRunTask

abstract class ScalaJSTestTask extends NonJvmRunTask.Test[ScalaJSLinkTask] with ScalaJSRunTask:
  final override protected def linkTaskClass: Class[ScalaJSLinkTestTask] = classOf[ScalaJSLinkTestTask]

  final override protected def backendKind: ScalaBackendKind = ScalaBackendKind.JS

  final override protected def createTestEnvironment: ScalaJSTestEnvironment =
    scalaJSRun.createTestEnvironment(
      jsDirectory = linkTask.getJSDirectory,
      node = linkTask.node,
      logSource = getName
    )
