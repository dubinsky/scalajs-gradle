package org.podval.tools.test

import org.gradle.api.internal.tasks.testing.TestDescriptorInternal

trait Test extends TestDescriptorInternal:
  def getParentId: Object
  final override def getParent: Test = null
  final override def getDisplayName: String = getName
  final override def getClassDisplayName: String = getClassName
