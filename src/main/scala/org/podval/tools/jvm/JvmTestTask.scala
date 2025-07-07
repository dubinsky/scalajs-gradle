package org.podval.tools.jvm

import org.podval.tools.build.TestEnvironment
import org.podval.tools.test.task.TestTask

abstract class JvmTestTask extends TestTask[JvmBackend.type]:
  override protected def testEnvironmentCreator: TestEnvironment.Creator[JvmBackend.type] = JvmBackend
