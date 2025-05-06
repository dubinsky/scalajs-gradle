package org.podval.tools.scalajsplugin.scalajs

import org.podval.tools.build.ScalaBackendKind
import org.podval.tools.scalajsplugin.nonjvm.NonJvmTestTask

abstract class ScalaJSTestTask extends NonJvmTestTask[ScalaJSLinkTask] with ScalaJSRunTask:
  final override protected def linkTaskClass: Class[ScalaJSLinkTestTask] = classOf[ScalaJSLinkTestTask]

  final override protected def backendKind: ScalaBackendKind = ScalaBackendKind.JS

  final override protected def createTestEnvironment: ScalaJSTestEnvironment = ScalaJSTestEnvironment(
    scalaJSRunCommon
  )
