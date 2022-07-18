package org.podval.tools.test

import sbt.testing.{Fingerprint, Selector}

trait FingerprintedTest(
  parentId: Object,
  id: Object,
  className: String,
  val fingerprint: Fingerprint,
  val explicitlySpecified: Boolean,
  val selectors: Array[Selector]
) extends Test:

  final override def getParentId: Object = parentId
  final override def getId: Object = id
  final override def getClassName: String = className
