package org.podval.tools.test.taskdef

import org.podval.tools.util.Scala212Collections.arrayForAll
import sbt.testing.{NestedSuiteSelector, NestedTestSelector, Selector, SuiteSelector, TestSelector, TestWildcardSelector}

object Selectors:
  // I can not rely on the test framework implementing `equals()` on `Selector`s correctly.
  def equal(left: Selector, right: Selector): Boolean = (left, right) match
    case (_: SuiteSelector, _: SuiteSelector) =>
      true
    case (left: TestSelector, right: TestSelector) =>
      left.testName == right.testName
    case (left: NestedSuiteSelector, right: NestedSuiteSelector) =>
      left.suiteId == right.suiteId
    case (left: NestedTestSelector, right: NestedTestSelector) =>
      left.suiteId == right.suiteId &&
      left.testName == right.testName
    case (left: TestWildcardSelector, right: TestWildcardSelector) =>
      left.testWildcard == right.testWildcard
    case _ => false

  def suiteId(selector: Selector): Option[String] = selector match
    case nestedSuiteSelector: NestedSuiteSelector => Option(nestedSuiteSelector.suiteId)
    case nestedTestSelector : NestedTestSelector  => Option(nestedTestSelector .suiteId)
    case _                                        => None

  def testName(selector: Selector): Option[String] = selector match
    case testSelector      : TestSelector       => Option(testSelector      .testName)
    case nestedTestSelector: NestedTestSelector => Option(nestedTestSelector.testName)
    case _                                      => None

  def isRunningSuite(selector: Selector): Boolean = selector match
    case _: SuiteSelector        => true
    case _: NestedSuiteSelector  => true
    case _: TestWildcardSelector => throw IllegalArgumentException(s"Can't be running $selector!")
    case _                       => false

  def isTest(selector: Selector): Boolean = selector match
    case _: TestSelector         => true
    case _: NestedTestSelector   => true
    case _: TestWildcardSelector => true
    case _                       => false

  def isEventForTest(selector: Selector): Boolean = selector match
    case _: TestSelector         => true
    case _: NestedTestSelector   => true
    case _: TestWildcardSelector => throw IllegalArgumentException(s"Illegal event selector $selector!")
    case _                       => false

  def nestedSelector(
    nestingSelector: Selector,
    nestedSelectors: Array[Selector]
  ): Selector =
    val canHaveNested: Boolean = nestingSelector match
      case _: SuiteSelector => true
      case _: NestedSuiteSelector => true
      case _ => false

    require(canHaveNested, s"$nestingSelector can not have nested tests!")

    require(nestedSelectors.length == 1, "Only one selector can be nested!")
    val selector: Selector = nestedSelectors(0)

    val canBeNested: Boolean = selector match
      case _: NestedSuiteSelector => true
      case _: NestedTestSelector  => true
      case _: TestSelector        => true // ScalaCheck does this ;)
      case _                      => false

    require(canBeNested, s"$selector can not be nested")

    selector
