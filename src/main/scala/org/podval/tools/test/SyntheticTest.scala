package org.podval.tools.test

abstract class SyntheticTest extends Test:
  final override def isComposite: Boolean = true
  final override def getClassName: String = null
  final override def getId: Object = getName
  final override def toString: String = getName
