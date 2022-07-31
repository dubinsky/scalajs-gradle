package org.podval.tools.test.serializer

import sbt.testing.SubclassFingerprint

final class SubclassFingerprintImpl(
  override val superclassName: String,
  override val isModule: Boolean,
  override val requireNoArgConstructor: Boolean
) extends SubclassFingerprint
