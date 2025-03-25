package org.podval.tools.test.taskdef

import sbt.testing.{NestedSuiteSelector, NestedTestSelector, Selector, SuiteSelector, TestSelector, TestWildcardSelector}

// I can not rely on the test framework implementing `equals()` on `Selector`s correctly.
object Selectors extends Ops[Selector](":"):
  object Many extends ArrayOps[Selector](Selectors, "--")

  override protected def toStrings(value: Selector): Array[String] = value match
    case _                   : SuiteSelector        => Array("Suite")
    case testSelector        : TestSelector         => Array("Test", testSelector.testName)
    case nestedSuiteSelector : NestedSuiteSelector  => Array("NestedSuite", nestedSuiteSelector.suiteId)
    case nestedTestSelector  : NestedTestSelector   => Array("NestedTest", nestedTestSelector.suiteId, nestedTestSelector.testName)
    case testWildcardSelector: TestWildcardSelector => Array("TestWildcard", testWildcardSelector.testWildcard)

  override protected def fromStrings(strings: Array[String]): Selector = strings(0) match
    case "Suite"        => SuiteSelector       ()
    case "Test"         => TestSelector        (strings(1))
    case "NestedSuite"  => NestedSuiteSelector (strings(1))
    case "NestedTest"   => NestedTestSelector  (strings(1), strings(2))
    case "TestWildcard" => TestWildcardSelector(strings(1))
