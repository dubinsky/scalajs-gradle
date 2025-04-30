package org.podval.tools.scalajsplugin.nonjvm

import org.gradle.api.DefaultTask

abstract class BackendRunMainTask[L <: BackendLinkTask[L]] extends DefaultTask with BackendRunTask[L]:
  setGroup("other")

  final override protected def flavourBase: String = "Run"
