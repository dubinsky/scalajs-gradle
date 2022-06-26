package org.podval.tools.scalajs

import org.gradle.api.internal.tasks.testing.TestDescriptorInternal

// TODO factor commonality with TestDefinition out into a common superclass
final class TestSuiteDescriptor(name: String) extends TestDescriptorInternal:
  override def isComposite: Boolean = true
  override def getName: String = name
  override def getDisplayName: String = name
  override def getClassName: String = name
  override def getClassDisplayName: String = name
  override def getParent: TestDescriptorInternal = null
  override def getId: Object = null
