package org.podval.tools.scalajsplugin.nonjvm

import org.gradle.api.DefaultTask

abstract class NonJvmLinkMainTask[L <: NonJvmLinkTask[L]] extends DefaultTask with NonJvmLinkTask[L]:
  this: L =>

  final override protected def flavourBase: String = "Link"

  final override protected def isTest: Boolean = false
