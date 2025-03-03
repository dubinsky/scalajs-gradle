package org.podval.tools.test.taskdef

import org.podval.tools.util.Scala212Collections.{arrayMap, arrayMkString}
import sbt.testing.{NestedSuiteSelector, NestedTestSelector, Selector, SuiteSelector, TestSelector, TestWildcardSelector}

object Selectors:
  def toString(selectors: Array[Selector]): String = arrayMkString(arrayMap(selectors, _.toString), "[", ", ", "]")

  def isTest(selector: Selector): Boolean = selector match
    case _: TestSelector | _: NestedTestSelector | _: TestWildcardSelector => true
    case _ => false
    
  // Selector subclasses are final and override equals(),
  // so `left.equals(right)` should work just fine,
  // but with ScalaCheck running on ScalaJS (but not plain Scala)
  // I get TestSelector(String.startsWith) != TestSelector(String.startsWith) -
  // for every test method, even other than `String.startsWith`, so...
  def equal(left: Selector, right: Selector): Boolean =
    val result: Boolean = (left, right) match
      case (_: SuiteSelector, _: SuiteSelector) => true
      case (left: NestedSuiteSelector, right: NestedSuiteSelector) => left.suiteId == right.suiteId
      case (left: TestSelector, right: TestSelector) => left.testName == right.testName
      case (left: NestedTestSelector, right: NestedTestSelector) => (left.suiteId == right.suiteId) && left.testName == right.testName()
      case (left: TestWildcardSelector, right: TestWildcardSelector) => left.testWildcard == right.testWildcard
      case _ => false

    //    require(result == left.equals(right),
    //      s"--- SELECTOR COMPARISON DISCREPANCY: $left [${left.getClass}] and $right [${right.getClass}]"
    //    )

    result
  
  def write(value: Selector): String = value match
    case suiteSelector       : SuiteSelector        => s"Suite"
    case testSelector        : TestSelector         => s"Test:${testSelector.testName}"
    case nestedSuiteSelector : NestedSuiteSelector  => s"NestedSuite:${nestedSuiteSelector.suiteId}"
    case nestedTestSelector  : NestedTestSelector   => s"NestedTest:${nestedTestSelector.suiteId}:${nestedTestSelector.testName}"
    case testWildcardSelector: TestWildcardSelector => s"TestWildcard:${testWildcardSelector.testWildcard}"

  def read(string: String): Selector =
    val parts: Array[String] = string.split(":")
    parts(0) match
      case "Suite"        => SuiteSelector       ()
      case "Test"         => TestSelector        (parts(1))
      case "NestedSuite"  => NestedSuiteSelector (parts(1))
      case "NestedTest"   => NestedTestSelector  (parts(1), parts(2))
      case "TestWildcard" => TestWildcardSelector(parts(1))
