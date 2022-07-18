package org.podval.tools.test

class SyntheticTest(
  parentId: Object,
  id: Object,
  name: String
) extends Test:
  final override def getParentId: Object = parentId
  final override def getId: AnyRef = id
  final override def isComposite: Boolean = true
  final override def getClassName: String = null
  final override def getName: String = name
  final override def toString: String = s"Synthetic Suite $name"
