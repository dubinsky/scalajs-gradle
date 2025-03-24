package org.podval.tools.test.detect

import sbt.testing.{Framework, SubclassFingerprint}

final class SubclassFingerprintDetector(
  fingerprint: SubclassFingerprint,
  framework: Framework
) extends FingerprintDetector(
  fingerprint,
  framework
):
  override def name: String = fingerprint.superclassName
  override def isModule: Boolean = fingerprint.isModule

