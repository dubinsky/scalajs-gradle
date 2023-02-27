package org.podval.tools.testing.serializer

import sbt.testing.AnnotatedFingerprint

final class AnnotatedFingerprintImpl(
  override val annotationName: String,
  override val isModule: Boolean
) extends AnnotatedFingerprint
