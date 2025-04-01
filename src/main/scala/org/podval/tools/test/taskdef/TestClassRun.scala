package org.podval.tools.test.taskdef

import org.gradle.api.internal.tasks.testing.TestClassRunInfo
import sbt.testing.Fingerprint

final class TestClassRun(
  val frameworkProvider: FrameworkProvider,
  override val getTestClassName: String,
  val fingerprint: Fingerprint,
  val explicitlySpecified: Boolean,
  val testNames: Array[String],
  val testWildCards: Array[String]
) extends TestClassRunInfo
  

object TestClassRun extends Ops[TestClassRun]("@"):
  override protected def toStrings(value: TestClassRun): Array[String] = Array(
    value.frameworkProvider.frameworkName,
    value.getTestClassName,
    Fingerprints.write(value.fingerprint),
    Ops.toString(value.explicitlySpecified),
    Strings.Many.write(value.testNames),
    Strings.Many.write(value.testWildCards)
  )

  override protected def fromStrings(strings: Array[String]): TestClassRun = TestClassRun(
    frameworkProvider = FrameworkProviderForking(frameworkName = strings(0)),
    getTestClassName = strings(1),
    fingerprint = Fingerprints.read(strings(2)),
    explicitlySpecified = Ops.toBoolean(strings(3)),
    testNames = Strings.Many.read(strings(4)),
    testWildCards = Strings.Many.read(strings(5))
  )
