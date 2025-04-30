package org.podval.tools.scalajsplugin.nonjvm

import org.gradle.api.DefaultTask

abstract class BackendLinkMainTask[L <: BackendLinkTask[L]] extends DefaultTask with BackendLinkTask[L]:
  this: L =>

  final override protected def flavourBase: String = "Link"

  final override protected def isTest: Boolean = false
