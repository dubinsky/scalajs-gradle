package org.podval.tools.test.detect

import sbt.testing.{AnnotatedFingerprint, Fingerprint, Framework, SubclassFingerprint}
import xsbt.api.Discovered

private sealed abstract class Detector(
  val fingerprint: Fingerprint,
  val framework: Framework
):
  def isAnnotation: Boolean
  def name: String
  def isModule: Boolean
  def names(discovered: Discovered): Set[String]
  
  final def is(discovered: Discovered): Boolean =
    (discovered.isModule == isModule) &&
    names(discovered).contains(name)
    
object Detector:
  private final class SubclassDetector(
    fingerprint: SubclassFingerprint,
    framework: Framework
  ) extends Detector(
    fingerprint,
    framework
  ):
    override def isAnnotation: Boolean = false
    override def name: String = fingerprint.superclassName
    override def isModule: Boolean = fingerprint.isModule
    override def names(discovered: Discovered): Set[String] = discovered.baseClasses
    
  private final class AnnotatedDetector(
    fingerprint: AnnotatedFingerprint,
    framework: Framework
  ) extends Detector(
    fingerprint,
    framework
  ):
    override def isAnnotation: Boolean = true
    override def name: String = fingerprint.annotationName
    override def isModule: Boolean = fingerprint.isModule
    override def names(discovered: Discovered): Set[String] = discovered.annotations

  def get(
    fingerprint: Fingerprint,
    framework: Framework
  ): Option[Detector] = fingerprint match
    case subclassFingerprint: SubclassFingerprint => Some(SubclassDetector(subclassFingerprint, framework))
    case annotatedFingerprint: AnnotatedFingerprint => Some(AnnotatedDetector(annotatedFingerprint, framework))
    case _ => None
