package org.podval.tools.scalajsplugin.jvm

import org.podval.tools.build.jvm.{JvmBackend, JvmTestEnvironment}
import org.podval.tools.scalajsplugin.BackendTask

abstract class JvmTestTask extends BackendTask.Run.Test with JvmTask:
  final override protected def backend: JvmBackend.type = JvmBackend
  
  final override protected def createTestEnvironment: JvmTestEnvironment = JvmTestEnvironment()
