package org.podval.tools.backendplugin.scalanative

import org.podval.tools.backend.scalanative.{ScalaNativeBuild, ScalaNativeTestEnvironment}
import org.podval.tools.backendplugin.nonjvm.NonJvmRunTask

abstract class ScalaNativeTestTask extends NonJvmRunTask.Test[ScalaNativeLinkTask] with ScalaNativeRunTask:
  final override protected def linkTaskClass: Class[ScalaNativeLinkTestTask] = classOf[ScalaNativeLinkTestTask]
  
  final override protected def createTestEnvironment: ScalaNativeTestEnvironment =
    ScalaNativeBuild.createTestEnvironment(
      binaryTestFile = linkTask.getOutputFile,
      logSource = getName
    )
