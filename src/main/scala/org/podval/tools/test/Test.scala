package org.podval.tools.test

import org.gradle.api.internal.tasks.testing.TestDescriptorInternal

abstract class Test extends TestDescriptorInternal:
  final override def getParent: Test = null
  final override def getDisplayName: String = getName
  final override def getClassDisplayName: String = getClassName

  def getParentId: Object
