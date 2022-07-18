package org.podval.tools.test

import sbt.testing.{Fingerprint, Selector, TestSelector}

// Note: in reality, an individual test is not always a method (e.g., in ScalaTest), but compared to a class it is :)
final class TestMethod(
  parent: TestClass,
  id: Object,
  methodName: String,
  fingerprint: Fingerprint,
  selectors: Array[Selector]
) extends FingerprintedTest(
  parentId = parent.getId,
  id = id,
  className = parent.getClassName,
  fingerprint = fingerprint,
  explicitlySpecified = false,
  selectors = selectors
):
  override def isComposite: Boolean = false
  override def getName: String = methodName
  override def toString: String = s"Test $getClassName.$getName"
