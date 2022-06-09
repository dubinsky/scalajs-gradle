package org.podval.tools.scalajs.testing

import sbt.testing.Logger as TLogger

// Note: based on sbt.TestReportListener from org.scala-sbt.testing
final class ContentLogger(val log: TLogger, val flush: () => Unit)
