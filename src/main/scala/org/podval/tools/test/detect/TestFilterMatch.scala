package org.podval.tools.test.detect

import sbt.testing.{Selector, SuiteSelector, TestSelector, TestWildcardSelector}

sealed trait TestFilterMatch derives CanEqual:
  def explicitlySpecified: Boolean
  def isEmpty: Boolean
  def selectors: Array[Selector]
  def intersect(that: TestFilterMatch): TestFilterMatch

object TestFilterMatch:
  final case class Suite(
    override val explicitlySpecified: Boolean
  ) extends TestFilterMatch:
    override def isEmpty: Boolean = false
    override def selectors: Array[Selector] = Array(new SuiteSelector)

    override def intersect(other: TestFilterMatch): TestFilterMatch = other match
      case that: Suite => Suite(this.explicitlySpecified || that.explicitlySpecified)
      case that: Tests => that

  final case class Tests(
    testNames: Set[String],
    testWildCards: Set[String]
  ) extends TestFilterMatch:
    override def explicitlySpecified: Boolean = true
    override def isEmpty: Boolean = testNames.isEmpty && testWildCards.isEmpty

    override def selectors: Array[Selector] =
      testNames    .toArray.map(TestSelector        (_)) ++
      testWildCards.toArray.map(TestWildcardSelector(_))

    override def intersect(other: TestFilterMatch): TestFilterMatch = other match
      case that: Suite => this
      case that: Tests => Tests(
        testNames = this.testNames.intersect(that.testNames) ++
          this.testNames.filter(l => that.testWildCards.exists(l.contains)), // TODO [filter]
        testWildCards = this.testWildCards.intersect(that.testWildCards)
      )
