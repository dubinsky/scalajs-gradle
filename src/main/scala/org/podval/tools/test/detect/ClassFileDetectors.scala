package org.podval.tools.test.detect

final class ClassFileDetectors private(private val detectors: List[FingerprintDetectors]):
  def add(detector: FingerprintDetector): ClassFileDetectors = ClassFileDetectors(
    if detectors.isEmpty
    then List(Set(detector))
    else List(detectors.head ++ Set(detector)) ++ detectors.tail
  )  

  def append(more: ClassFileDetectors): ClassFileDetectors = ClassFileDetectors(
    detectors ++ more.detectors
  )

  def add(that: ClassFileDetectors): ClassFileDetectors = ClassFileDetectors(
    this.detectors.zipAll(that.detectors, Set.empty, Set.empty).map(_ ++ _)
  )
  
  // Returns detectors closest to the class in the hierarchy.
  def getApplicable(isModule: Boolean): FingerprintDetectors = detectors
    .map(_.filter(_.isModule == isModule))
    .find(_.nonEmpty)
    .getOrElse(Set.empty)

object ClassFileDetectors:
  val empty: ClassFileDetectors = ClassFileDetectors(List.empty)
