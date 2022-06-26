package org.podval.tools.scalajs

import org.gradle.api.tasks.testing.TestResult.ResultType
import sbt.testing.{Fingerprint, Selector} // SBT: test-interface

final class TestEvent(
  val fullyQualifiedName: String,
  val fingerprint: Fingerprint, // TODO remove
  val selector: Selector,
  val status: ResultType,
  val throwable: Option[Throwable],
  val duration: Long
)
