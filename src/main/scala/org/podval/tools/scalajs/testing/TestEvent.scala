package org.podval.tools.scalajs.testing

import org.gradle.api.tasks.testing.TestResult.ResultType
import sbt.testing.{Event as TEvent, Fingerprint, OptionalThrowable, Selector, Status as TStatus}

final class TestEvent(
  val fullyQualifiedName: String,
  val fingerprint: Fingerprint,
  val selector: Selector,
  val status: ResultType,
  val throwable: OptionalThrowable,
  val duration: Long
)

