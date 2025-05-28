package org.podval.tools.scalaplugin.jvm

import org.podval.tools.build.jvm.JvmTestEnvironment
import org.podval.tools.scalaplugin.BackendTask

abstract class JvmTestTask extends BackendTask.Run.Test with JvmTask:
  final override protected def createTestEnvironment: JvmTestEnvironment = JvmTestEnvironment()
