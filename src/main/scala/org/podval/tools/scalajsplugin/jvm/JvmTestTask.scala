package org.podval.tools.scalajsplugin.jvm

import org.podval.tools.build.ScalaBackendKind
import org.podval.tools.scalajsplugin.BackendTask

abstract class JvmTestTask extends BackendTask.Run.Test with JvmTask:
  final override protected def backendKind: ScalaBackendKind = ScalaBackendKind.JVM
  
  final override protected def createTestEnvironment: JvmTestEnvironment = JvmTestEnvironment()
