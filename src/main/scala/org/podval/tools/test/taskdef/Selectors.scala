package org.podval.tools.test.taskdef

import sbt.testing.{NestedSuiteSelector, NestedTestSelector, Selector, SuiteSelector, TestSelector, TestWildcardSelector}

object Selectors:
  // Although `Selector` subclasses do implement `equals()`, it does not seem to work for some reason:
  // TestSelector(org.podval.tools.test.JUnit4Test.assumeFalse) != TestSelector(org.podval.tools.test.JUnit4Test.assumeFalse)
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
