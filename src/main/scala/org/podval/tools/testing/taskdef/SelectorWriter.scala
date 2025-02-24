package org.podval.tools.testing.taskdef

import sbt.testing.{NestedSuiteSelector, NestedTestSelector, Selector, SuiteSelector, TestSelector, TestWildcardSelector}

object SelectorWriter:
  def write(value: Selector): String = value match
    case suiteSelector       : SuiteSelector        => s"Suite"
    case testSelector        : TestSelector         => s"Test:${testSelector.testName}"
    case nestedSuiteSelector : NestedSuiteSelector  => s"NestedSuite:${nestedSuiteSelector.suiteId}"
    case nestedTestSelector  : NestedTestSelector   => s"NestedTest:${nestedTestSelector.suiteId}:${nestedTestSelector.testName}"
    case testWildcardSelector: TestWildcardSelector => s"TestWildcard:${testWildcardSelector.testWildcard}"

  def read(string: String): Selector =
    val parts: Array[String] = string.split(':')
    parts(0) match
      case "Suite"        => SuiteSelector       ()
      case "Test"         => TestSelector        (parts(1))
      case "NestedSuite"  => NestedSuiteSelector (parts(1))
      case "NestedTest"   => NestedTestSelector  (parts(1), parts(2))
      case "TestWildcard" => TestWildcardSelector(parts(1))
