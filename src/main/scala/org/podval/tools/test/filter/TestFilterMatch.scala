package org.podval.tools.test.filter

trait TestFilterMatch derives CanEqual:
  def explicitlySpecified: Boolean
  def isEmpty: Boolean
  def intersect(that: TestFilterMatch): TestFilterMatch
