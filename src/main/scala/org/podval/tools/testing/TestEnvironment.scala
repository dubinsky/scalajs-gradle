package org.podval.tools.testing

import sbt.testing.Framework
import java.io.File

// Note: based on org.scalajs.testing.adapter.TestAdapter
abstract class TestEnvironment:
  def loadFrameworks(testClassPath: Iterable[File]): List[Framework]
  
  def close(): Unit
