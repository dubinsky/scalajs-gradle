package org.podval.tools.test

import org.podval.tools.test.framework.FrameworkDescriptor
import sbt.testing.Framework

// Note: based on org.scalajs.testing.adapter.TestAdapter
abstract class TestEnvironment:
  final def loadAllFrameworks: List[Framework] = loadFrameworks(FrameworkDescriptor.all)

  def loadFrameworks(descriptors: List[FrameworkDescriptor]): List[Framework]

  def close(): Unit
