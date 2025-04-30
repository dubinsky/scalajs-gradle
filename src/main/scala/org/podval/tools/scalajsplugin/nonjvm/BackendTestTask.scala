package org.podval.tools.scalajsplugin.nonjvm

import org.podval.tools.test.task.TestTask

abstract class BackendTestTask[L <: BackendLinkTask[L]] extends TestTask with BackendRunTask[L]:
  final override protected def flavourBase: String = "Test"
