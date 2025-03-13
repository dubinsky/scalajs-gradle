package org.podval.tools.test.filter

import sbt.testing.{Selector, TestSelector, TestWildcardSelector}

final case class TestsTestFilterMatch(
  testNames: Set[String],
  testWildCards: Set[String]
) extends TestFilterMatch:
  override def explicitlySpecified: Boolean = true

  override def isEmpty: Boolean = testNames.isEmpty && testWildCards.isEmpty

  override def selectors: Array[Selector] =
    testNames.toArray.map(TestSelector(_)) ++
    testWildCards.toArray.map(TestWildcardSelector(_))

  override def intersect(other: TestFilterMatch): TestFilterMatch = other match
    case that: SuiteTestFilterMatch => this
    case that: TestsTestFilterMatch => TestsTestFilterMatch(
      testNames = this.testNames.intersect(that.testNames) ++
        this.testNames.filter(l => that.testWildCards.exists(l.contains)), // TODO [filter]
      testWildCards = this.testWildCards.intersect(that.testWildCards)
    )
