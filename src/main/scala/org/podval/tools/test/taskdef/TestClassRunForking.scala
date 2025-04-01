package org.podval.tools.test.taskdef

import sbt.testing.{Fingerprint, Framework}

final class TestClassRunForking(
  override val frameworkName: String,
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
  override lazy val framework: Framework = frameworkDescriptor.newInstance.asInstanceOf[Framework]
