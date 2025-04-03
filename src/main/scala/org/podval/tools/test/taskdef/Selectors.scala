package org.podval.tools.test.taskdef

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
