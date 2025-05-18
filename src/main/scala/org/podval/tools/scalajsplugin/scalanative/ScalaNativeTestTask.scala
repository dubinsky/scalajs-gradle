package org.podval.tools.scalajsplugin.scalanative

import org.podval.tools.build.scalanative.{ScalaNativeBackend, ScalaNativeBuild, ScalaNativeTestEnvironment}
import org.podval.tools.scalajsplugin.nonjvm.NonJvmRunTask

abstract class ScalaNativeTestTask extends NonJvmRunTask.Test[ScalaNativeLinkTask] with ScalaNativeRunTask:
  final override protected def linkTaskClass: Class[ScalaNativeLinkTestTask] = classOf[ScalaNativeLinkTestTask]

  final override protected def backend: ScalaNativeBackend.type = ScalaNativeBackend

  final override protected def createTestEnvironment: ScalaNativeTestEnvironment =
    ScalaNativeBuild.createTestEnvironment(
      binaryTestFile = linkTask.getOutputFile,
      logSource = getName
    )
