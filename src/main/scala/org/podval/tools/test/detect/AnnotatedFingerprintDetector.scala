package org.podval.tools.test.detect

import org.podval.tools.build.TestFramework
import sbt.testing.AnnotatedFingerprint

private final class AnnotatedFingerprintDetector(
  fingerprint: AnnotatedFingerprint,
  framework: TestFramework.Loaded
) extends FingerprintDetector(
  fingerprint,
  framework
):
  override def name: String = fingerprint.annotationName
  override def isModule: Boolean = fingerprint.isModule
