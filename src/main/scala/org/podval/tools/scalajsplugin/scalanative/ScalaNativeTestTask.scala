package org.podval.tools.scalajsplugin.scalanative

import org.podval.tools.build.ScalaBackendKind
import org.podval.tools.scalajsplugin.nonjvm.NonJvmTestTask

abstract class ScalaNativeTestTask extends NonJvmTestTask[ScalaNativeLinkTask] with ScalaNativeRunTask:
  final override protected def linkTaskClass: Class[ScalaNativeLinkTestTask] = classOf[ScalaNativeLinkTestTask]
  
  final override protected def backendKind: ScalaBackendKind = ScalaBackendKind.Native

  final override protected def createTestEnvironment: ScalaNativeTestEnvironment = ScalaNativeTestEnvironment(
    linkTask.getOutputFile
  )
