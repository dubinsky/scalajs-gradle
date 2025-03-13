package org.podval.tools.test.detect

import sbt.testing.{AnnotatedFingerprint, Framework}
import xsbt.api.Discovered

private final class AnnotatedFingerprintDetector(
  fingerprint: AnnotatedFingerprint,
  framework: Framework
) extends FingerprintDetector(
  fingerprint,
  framework
):
  override def name: String = fingerprint.annotationName
  override def isModule: Boolean = fingerprint.isModule
  override protected def names(discovered: Discovered): Set[String] = discovered.annotations

