package org.podval.tools.test.taskdef

import org.podval.tools.util.Scala212Collections.arrayForAll
import sbt.testing.{NestedSuiteSelector, NestedTestSelector, Selector, SuiteSelector, TestSelector, TestWildcardSelector}

// TODO remove Ops and implement equal/toString
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
