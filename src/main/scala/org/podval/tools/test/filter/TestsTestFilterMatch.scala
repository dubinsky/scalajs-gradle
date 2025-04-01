package org.podval.tools.test.filter

final case class TestsTestFilterMatch(
  testNames: Set[String],
  testWildCards: Set[String]
) extends TestFilterMatch:
  override def explicitlySpecified: Boolean = true

  override def isEmpty: Boolean = testNames.isEmpty && testWildCards.isEmpty

  override def intersect(other: TestFilterMatch): TestFilterMatch = other match
    case _   : SuiteTestFilterMatch => this
    case that: TestsTestFilterMatch => TestsTestFilterMatch(
      testNames = this.testNames.intersect(that.testNames) ++
        this.testNames.filter(l => that.testWildCards.exists(l.contains)), // TODO [filter]
      testWildCards = this.testWildCards.intersect(that.testWildCards)
    )
