package org.podval.tools.scalajsplugin.jvm

import org.podval.tools.build.ScalaBackendKind
import org.podval.tools.test.task.TestTask

abstract class JvmTestTask extends TestTask:
  setDescription(s"Test using sbt frameworks")

  final override protected def backendKind: ScalaBackendKind = ScalaBackendKind.JVM
  
  final override protected def createTestEnvironment: JvmTestEnvironment = JvmTestEnvironment()
