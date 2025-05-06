package org.podval.tools.scalajsplugin.nonjvm

import org.podval.tools.test.task.TestTask

abstract class NonJvmTestTask[L <: NonJvmLinkTask[L]] extends TestTask with NonJvmRunTask[L]:
  final override protected def flavourBase: String = "Test"
