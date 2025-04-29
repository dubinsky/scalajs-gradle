package org.podval.tools.scalajsplugin.scalanative

import org.podval.tools.build.ScalaBackendKind
import org.podval.tools.test.task.TestTask

abstract class ScalaNativeTestTask extends TestTask:
  final override protected def backendKind: ScalaBackendKind = ScalaBackendKind.Native

  final override protected def createTestEnvironment: ScalaNativeTestEnvironment = ScalaNativeTestEnvironment()
