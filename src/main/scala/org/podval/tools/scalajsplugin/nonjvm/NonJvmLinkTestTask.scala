package org.podval.tools.scalajsplugin.nonjvm

abstract class NonJvmLinkTestTask[L <: NonJvmLinkTask[L]] extends NonJvmLinkTask[L]:
  this: L =>
  
  final override protected def isTest: Boolean = false
