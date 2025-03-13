package org.podval.tools.test.detect

import org.podval.tools.test.taskdef.Fingerprints
import sbt.testing.{Fingerprint, Framework}
import xsbt.api.Discovered

abstract class FingerprintDetector(
  val fingerprint: Fingerprint,
  val framework: Framework
):
  final override def toString: String = Fingerprints.toString(fingerprint)

  final override def equals(other: Any): Boolean = other match
    case that: FingerprintDetector =>
      this.framework.name == that.framework.name &&
      Fingerprints.equal(this.fingerprint, that.fingerprint)
    case _ => false

  final override def hashCode: Int = 37*framework.name.hashCode + fingerprint.hashCode
  
  final def isDiscovered(discovered: Discovered): Boolean =
    (discovered.isModule == isModule) &&
    names(discovered).contains(name)

  protected def names(discovered: Discovered): Set[String]
      
  def isModule: Boolean

  def name: String
  