package org.podval.tools.test.detect

import org.podval.tools.build.TestFramework
import org.podval.tools.test.run.Fingerprints
import sbt.testing.Fingerprint

object FingerprintDetector:
  type Many = Set[FingerprintDetector]

abstract class FingerprintDetector(
  val fingerprint: Fingerprint,
  val framework: TestFramework.Loaded
):
  final override def toString: String = Fingerprints.toString(fingerprint)

  final override def equals(other: Any): Boolean = other.asInstanceOf[Matchable] match
    case that: FingerprintDetector =>
      this.framework.nameSbt == that.framework.nameSbt &&
      Fingerprints.equal(this.fingerprint, that.fingerprint)
    case _ => false

  final override def hashCode: Int = 37*framework.nameSbt.hashCode + fingerprint.hashCode
      
  def isModule: Boolean

  def name: String
