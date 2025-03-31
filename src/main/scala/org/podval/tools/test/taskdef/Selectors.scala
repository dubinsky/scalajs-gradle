package org.podval.tools.test.taskdef

import org.podval.tools.util.Scala212Collections.arrayForAll
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

  // see TestFilterMatch
  def fromTestFilterMatch(selectors: Array[Selector]): Selector =
    val isAllTests: Boolean = arrayForAll(selectors, isTestFromTestFilterMatch)

    if !isAllTests then
      require(selectors.length == 1, "If not all selectors are tests, there can only be one!")
      val selector: Selector = selectors(0)
      require(equal(selector, SuiteSelector()), s"If not all selectors are tests, there can only be SuiteSelector, not $selector!")

    if isAllTests then SuiteSelector() else selectors(0)

  private def isTestFromTestFilterMatch(selector: Selector): Boolean = selector match
    case _: SuiteSelector => false
    case _: NestedSuiteSelector => throw IllegalArgumentException(s"NestedSuiteSelector can not be a part of TestFilterMatch.")
    case _: TestSelector => true
    case _: NestedTestSelector => throw IllegalArgumentException(s"NestedTestSelector can not be a part of TestFilterMatch.")
    case _: TestWildcardSelector => true

  // attribute nested test cases to the nested, not the nesting, suite
  def testClassAndTestName(
    className: String,
    selector: Selector,
    frameworkIncludesClassNameInTestName: Boolean
  ): (String, Option[String]) =
    val suiteId: String = selector match
      case nestedSuiteSelector: NestedSuiteSelector => nestedSuiteSelector.suiteId
      case nestedTestSelector : NestedTestSelector  => nestedTestSelector .suiteId
      case _ => className

    selector match
      case testSelector      : TestSelector       => Some(testSelector      .testName)
      case nestedTestSelector: NestedTestSelector => Some(nestedTestSelector.testName)
      case _ => None
    match
      case None => (suiteId, None)
      case Some(testName) =>
        // JUnit4 and its friends stick the class name in front of the method name;
        // we use the class name to attribute the test to:
        val lastDot: Int =
          if !frameworkIncludesClassNameInTestName
          then -1
          else testName.lastIndexOf('.')
        val testClassName: String =
          if lastDot == -1
          then suiteId
          else testName.substring(0, lastDot)  
        val testMethod: String =
          if lastDot == -1
          then testName
          else testName.substring(lastDot + 1)
        (testClassName, Some(testMethod))
  
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
      case _: NestedTestSelector => true
      case _: TestSelector => true // ScalaCheck does this ;)
      case _ => false
      
    require(canBeNested, s"$selector can not be nested")
    
    selector
  
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
