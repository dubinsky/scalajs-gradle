package org.podval.tools.test.detect

import org.podval.tools.test.framework.Framework
import sbt.testing.AnnotatedFingerprint

private final class AnnotatedFingerprintDetector(
  fingerprint: AnnotatedFingerprint,
  framework: Framework.Loaded
) extends FingerprintDetector(
  fingerprint,
  framework
):
  override def name: String = fingerprint.annotationName
  override def isModule: Boolean = fingerprint.isModule
