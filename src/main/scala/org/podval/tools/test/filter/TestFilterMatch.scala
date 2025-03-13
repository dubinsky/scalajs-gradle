package org.podval.tools.test.filter

import sbt.testing.Selector

trait TestFilterMatch derives CanEqual:
  def explicitlySpecified: Boolean
  def isEmpty: Boolean
  def selectors: Array[Selector]
  def intersect(that: TestFilterMatch): TestFilterMatch
