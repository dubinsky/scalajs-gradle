package org.podval.tools.test.detect

import org.podval.tools.test.framework.Framework
import sbt.testing.SubclassFingerprint

final class SubclassFingerprintDetector(
  fingerprint: SubclassFingerprint,
  framework: Framework.Loaded
) extends FingerprintDetector(
  fingerprint,
  framework
):
  override def name: String = fingerprint.superclassName
  override def isModule: Boolean = fingerprint.isModule
