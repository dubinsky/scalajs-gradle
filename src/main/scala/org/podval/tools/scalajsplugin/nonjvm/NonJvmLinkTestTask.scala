package org.podval.tools.scalajsplugin.nonjvm

import org.gradle.api.DefaultTask

abstract class NonJvmLinkTestTask[L <: NonJvmLinkTask[L]] extends DefaultTask with NonJvmLinkTask[L]:
  this: L =>

  final override protected def flavourBase: String = "LinkTest"

  final override protected def isTest: Boolean = false
