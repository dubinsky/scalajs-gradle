package org.podval.tools.test.taskdef

import sbt.testing.AnnotatedFingerprint

private final class AnnotatedFingerprintImpl(
  override val annotationName: String,
  override val isModule: Boolean
) extends AnnotatedFingerprint
