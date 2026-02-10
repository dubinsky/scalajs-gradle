package org.podval.tools.jvm

import org.gradle.api.tasks.CacheableTask
import org.podval.tools.build.{TestEnvironment, TestTask}

@CacheableTask
abstract class JvmTestTask extends TestTask[JvmBackend.type]:
  final override protected def testEnvironmentCreator: TestEnvironment.Creator[JvmBackend.type] = JvmBackend
