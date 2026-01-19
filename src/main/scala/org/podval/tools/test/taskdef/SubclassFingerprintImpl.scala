package org.podval.tools.test.taskdef

import sbt.testing.SubclassFingerprint

private final class SubclassFingerprintImpl(
  override val superclassName: String,
  override val isModule: Boolean,
  override val requireNoArgConstructor: Boolean
) extends SubclassFingerprint:
  override def toString: String = s"SubclassFingerprint($superclassName, isModule=$isModule, requireNoArgConstructor=$requireNoArgConstructor)"
