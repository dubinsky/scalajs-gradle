package org.podval.tools.testing.taskdef

import sbt.testing.SubclassFingerprint

final class SubclassFingerprintImpl(
  override val superclassName: String,
  override val isModule: Boolean,
  override val requireNoArgConstructor: Boolean
) extends SubclassFingerprint
