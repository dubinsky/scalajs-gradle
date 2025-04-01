package org.podval.tools.test.taskdef

import sbt.testing.{Fingerprint, Framework}

final class TestClassRunNonForking(
  override val framework: Framework,
  getTestClassName: String,
  fingerprint: Fingerprint,
  explicitlySpecified: Boolean,
  testNames: Array[String],
  testWildCards: Array[String]
) extends TestClassRun(
  getTestClassName,
  fingerprint,
  explicitlySpecified,
  testNames,
  testWildCards
):
  override def frameworkName: String = framework.name
