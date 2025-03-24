package org.podval.tools.test.detect

import sbt.testing.{AnnotatedFingerprint, Framework}

private final class AnnotatedFingerprintDetector(
  fingerprint: AnnotatedFingerprint,
  framework: Framework
) extends FingerprintDetector(
  fingerprint,
  framework
):
  override def name: String = fingerprint.annotationName
  override def isModule: Boolean = fingerprint.isModule

