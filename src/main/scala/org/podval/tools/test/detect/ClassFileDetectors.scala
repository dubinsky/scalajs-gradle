package org.podval.tools.test.detect

final class ClassFileDetectors private(private val detectors: List[Set[FingerprintDetector]]):
  def add(more: Set[FingerprintDetector]): ClassFileDetectors = ClassFileDetectors(
    if detectors.isEmpty
    then List(more)
    else List(detectors.head ++ more) ++ detectors.tail
  )  

  def add(more: ClassFileDetectors): ClassFileDetectors =
    ClassFileDetectors(detectors ++ more.detectors)

  // Note: returns detectors closest to the class in the hierarchy,
  // so that if multiple detectors apply, but they are at different levels (e.g., MUnit and JUnit),
  // we still avoid ambiguity (e.g., choose MUnit).
  def getApplicableDetectors(isModule: Boolean): Set[FingerprintDetector] = detectors
    .map(_.filter(_.isModule == isModule))
    .find(_.nonEmpty)
    .getOrElse(Set.empty)

object ClassFileDetectors:
  val empty: ClassFileDetectors = ClassFileDetectors(List.empty)
  