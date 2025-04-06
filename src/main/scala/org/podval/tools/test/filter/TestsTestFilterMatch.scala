package org.podval.tools.test.filter

final case class TestsTestFilterMatch(
  testNames: Set[String],
  testWildcards: Set[String]
) extends TestFilterMatch:
  override def explicitlySpecified: Boolean = false

  override def isEmpty: Boolean = testNames.isEmpty && testWildcards.isEmpty

  override def intersect(other: TestFilterMatch): TestFilterMatch = other match
    case _   : SuiteTestFilterMatch => this
    case that: TestsTestFilterMatch => TestsTestFilterMatch(
      testNames = this.testNames.intersect(that.testNames) ++
        this.testNames.filter(l => that.testWildcards.exists(l.contains)), // TODO [filter]
      testWildcards = this.testWildcards.intersect(that.testWildcards)
    )
