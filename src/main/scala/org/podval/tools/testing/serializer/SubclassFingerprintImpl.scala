package org.podval.tools.testing.serializer

import sbt.testing.SubclassFingerprint

final class SubclassFingerprintImpl(
  override val superclassName: String,
  override val isModule: Boolean,
  override val requireNoArgConstructor: Boolean
) extends SubclassFingerprint
