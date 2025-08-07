package org.podval.tools.jvm

import org.podval.tools.test.task.{TestEnvironment, TestTask}

abstract class JvmTestTask extends TestTask[JvmBackend.type]:
  final override protected def testEnvironmentCreator: TestEnvironment.Creator[JvmBackend.type] = JvmBackend
