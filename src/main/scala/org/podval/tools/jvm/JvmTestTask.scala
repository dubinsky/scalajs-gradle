package org.podval.tools.jvm

import org.podval.tools.build.RunTask

abstract class JvmTestTask extends RunTask.Test:
  final override protected def createTestEnvironment: JvmBackend.TestEnvironment = JvmBackend.createTestEnvironment
