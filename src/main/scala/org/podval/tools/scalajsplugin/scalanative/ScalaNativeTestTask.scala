package org.podval.tools.scalajsplugin.scalanative

import org.podval.tools.build.ScalaBackendKind
import org.podval.tools.scalajsplugin.nonjvm.NonJvmRunTask
import org.podval.tools.scalanative.{ScalaNativeBuild, ScalaNativeTestEnvironment}

abstract class ScalaNativeTestTask extends NonJvmRunTask.Test[ScalaNativeLinkTask] with ScalaNativeRunTask:
  final override protected def linkTaskClass: Class[ScalaNativeLinkTestTask] = classOf[ScalaNativeLinkTestTask]

  final override protected def backendKind: ScalaBackendKind = ScalaBackendKind.Native

  final override protected def createTestEnvironment: ScalaNativeTestEnvironment =
    ScalaNativeBuild.createTestEnvironment(
      binaryTestFile = linkTask.getOutputFile
    )
