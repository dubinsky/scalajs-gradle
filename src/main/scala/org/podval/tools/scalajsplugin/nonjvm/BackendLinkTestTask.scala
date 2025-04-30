package org.podval.tools.scalajsplugin.nonjvm

import org.gradle.api.DefaultTask

abstract class BackendLinkTestTask[L <: BackendLinkTask[L]] extends DefaultTask with BackendLinkTask[L]:
  this: L =>

  final override protected def flavourBase: String = "LinkTest"

  final override protected def isTest: Boolean = false
