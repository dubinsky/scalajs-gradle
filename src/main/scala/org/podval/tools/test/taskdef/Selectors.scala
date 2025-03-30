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

  def testName(selector: Selector): Option[String] = selector match
    case testSelector: TestSelector => Some(testSelector.testName)
    case nestedTestSelector: NestedTestSelector => Some(nestedTestSelector.testName)
    case _ => None
    
  def suiteId(selector: Selector) = selector match
    case nestedSuiteSelector: NestedSuiteSelector => Some(nestedSuiteSelector.suiteId)
    case nestedTestSelector: NestedTestSelector => Some(nestedTestSelector.suiteId)
    case _ => None
      
  def canHaveNested(selector: Selector): Boolean = selector match
    case _: SuiteSelector => true
    case _: NestedSuiteSelector => true
    case _ => false

  def canBeNested(selector: Selector): Boolean = selector match
    case _: NestedSuiteSelector => true
    case _: NestedTestSelector => true
    case _: TestSelector => true // ScalaCheck does this ;)
    case _ => false

  def isTestFromTestFilterMatch(selector: Selector): Boolean = selector match
    case _: SuiteSelector => false
    case _: NestedSuiteSelector => throw IllegalArgumentException(s"NestedSuiteSelector can not be a part of TestFilterMatch.")
    case _: TestSelector => true
    case _: NestedTestSelector => throw IllegalArgumentException(s"NestedTestSelector can not be a part of TestFilterMatch.")
    case _: TestWildcardSelector => true
    
  def isTest(selector: Selector): Boolean = selector match
    case _: SuiteSelector => false
    case _: NestedSuiteSelector => false
    case _: TestSelector => true
    case _: NestedTestSelector => true
    case _: TestWildcardSelector => true
    
  def isRunningSuite(selector: Selector): Boolean = selector match
    case _: SuiteSelector => true
    case _: NestedSuiteSelector => true
    case _: TestSelector => false
    case _: NestedTestSelector => false
    case wildcard: TestWildcardSelector => throw IllegalArgumentException(s"Can't be running $wildcard!")

  def isEventForTest(selector: Selector): Boolean = selector match
    case _: SuiteSelector => false
    case _: NestedSuiteSelector => false
    case _: TestSelector => true
    case _: NestedTestSelector => true
    case wildcard: TestWildcardSelector => throw IllegalArgumentException(s"Illegal event selector $wildcard!")
