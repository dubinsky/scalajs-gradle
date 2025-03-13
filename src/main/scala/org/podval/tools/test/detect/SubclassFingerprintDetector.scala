package org.podval.tools.test.detect

import sbt.testing.{Framework, SubclassFingerprint}
import xsbt.api.Discovered

final class SubclassFingerprintDetector(
  fingerprint: SubclassFingerprint,
  framework: Framework
) extends FingerprintDetector(
  fingerprint,
  framework
):
  override def name: String = fingerprint.superclassName
  override def isModule: Boolean = fingerprint.isModule
  override protected def names(discovered: Discovered): Set[String] = discovered.baseClasses

