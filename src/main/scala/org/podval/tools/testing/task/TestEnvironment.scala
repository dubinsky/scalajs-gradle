package org.podval.tools.testing.task

import org.podval.tools.testing.framework.FrameworkDescriptor
import sbt.testing.Framework

// Note: based on org.scalajs.testing.adapter.TestAdapter
abstract class TestEnvironment:
  final def loadAllFrameworks: List[Framework] = loadFrameworks(FrameworkDescriptor.all)

  def loadFrameworks(descriptors: List[FrameworkDescriptor]): List[Framework]

  def close(): Unit
