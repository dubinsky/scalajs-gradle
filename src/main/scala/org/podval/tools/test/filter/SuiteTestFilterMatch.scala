package org.podval.tools.test.filter

final case class SuiteTestFilterMatch(
  override val explicitlySpecified: Boolean
) extends TestFilterMatch:
  override def isEmpty: Boolean = false
  
  override def intersect(other: TestFilterMatch): TestFilterMatch = other match
    case that: SuiteTestFilterMatch => SuiteTestFilterMatch(this.explicitlySpecified || that.explicitlySpecified)
    case that: TestsTestFilterMatch => that
