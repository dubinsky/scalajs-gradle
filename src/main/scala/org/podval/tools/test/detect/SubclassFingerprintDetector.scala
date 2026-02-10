package org.podval.tools.test.detect

import org.podval.tools.build.TestFramework
import sbt.testing.SubclassFingerprint

private final class SubclassFingerprintDetector(
  fingerprint: SubclassFingerprint,
  framework: TestFramework.Loaded
) extends FingerprintDetector(
  fingerprint,
  framework
):
  override def name: String = fingerprint.superclassName
  override def isModule: Boolean = fingerprint.isModule
