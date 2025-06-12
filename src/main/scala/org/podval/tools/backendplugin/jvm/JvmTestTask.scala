package org.podval.tools.backendplugin.jvm

import org.podval.tools.backend.jvm.JvmTestEnvironment
import org.podval.tools.backendplugin.BackendTask

abstract class JvmTestTask extends BackendTask.Run.Test with JvmTask:
  final override protected def createTestEnvironment: JvmTestEnvironment = JvmTestEnvironment()
