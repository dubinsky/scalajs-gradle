package org.podval.tools.scalajsplugin.jvm

import org.podval.tools.test.task.TestTask

abstract class JvmTestTask extends TestTask:
  setDescription(s"Test using sbt frameworks")
  
  final override protected def canFork: Boolean = true

  final override protected def canUseClassfileTestDetection: Boolean = true

  final override protected def testEnvironment: JvmTestEnvironment = JvmTestEnvironment()
