package org.podval.tools.scalajs.testing

import sbt.testing.Logger as TLogger

// Note: based on sbt.TestReportListener from org.scala-sbt.testing
trait ContentLogger:
  def log: TLogger

  def flush(): Unit
