package org.podval.tools.test.run

import org.podval.tools.test.taskdef.{Selectors, TestClassRun}
import org.podval.tools.util.Scala212Collections.{arrayConcat, arrayMap}
import sbt.testing.{Event, NestedSuiteSelector, NestedTestSelector, Selector, SuiteSelector, Task, TestSelector,
  TestWildcardSelector}

// Wrapper around Selector.
sealed trait Running:
  def selector: Selector
  def selectors: Array[Selector]
  def isSuite: Boolean
  def isTest : Boolean
  def isTests: Boolean
  def suiteId : Option[String]
  def testName: Option[String]

  final def sameAs(that: Running): Boolean = Selectors.equal(this.selector, that.selector)

  // JUnit4 emits overall class failure events with a `TestSelector`.
  final def isTestAndNotForClass(className: String): Boolean =
    isTest && !Selectors.equal(selector, TestSelector(className))

  // attribute nested test cases to the nested, not the nesting, suite;
  // JUnit4 and its friends stick the class name in front of the method name.  
  final def testClassAndTestName(
    frameworkIncludesClassNameInTestName: Boolean,
    className: String
  ): (String, Option[String]) =
    val (classNamePart: Option[String], testNamePart: Option[String]) = testName match
      case None => (None, None)
      case Some(testName) =>
        val lastDot: Int = testName.lastIndexOf('.')
        if !frameworkIncludesClassNameInTestName || lastDot == -1
        then (None, Some(testName))
        else (Some(testName.substring(0, lastDot)), Some(testName.substring(lastDot + 1)))

    (classNamePart.orElse(suiteId).getOrElse(className), testNamePart)

  final def forNestedTask(nestedTask: Task): Running =
    require(isSuite, s"$selector can not have nested tests!")

    val nestedSelectors: Array[Selector] = nestedTask.taskDef.selectors
    require(nestedSelectors.length == 1, "Only one selector can be nested!")

    nestedSelectors(0) match
      case nestedSuiteSelector: NestedSuiteSelector => Running.NestedSuite(nestedSuiteSelector)
      case nestedTestSelector : NestedTestSelector  => Running.NestedTest(nestedTestSelector)
      case testSelector       : TestSelector        => Running.Test(testSelector) // ScalaCheck does this ;)
      case selector => throw IllegalArgumentException(s"$selector can not be nested")

object Running:
  final case class Tests(override val selectors: Array[Selector]) extends Running:
    override def selector = SuiteSelector()
    override def isSuite: Boolean = true
    override def isTest : Boolean = false
    override def isTests: Boolean = true
    override def suiteId : Option[String] = None
    override def testName: Option[String] = None

  sealed abstract class One(override val selector: Selector) extends Running:
    final override def selectors: Array[Selector] = Array(selector)

  sealed trait SuiteLike extends Running:
    final override def isSuite: Boolean = true
    final override def isTest : Boolean = false
    final override def isTests: Boolean = false

  sealed trait TestLike extends Running:
    final override def isSuite: Boolean = false
    final override def isTest : Boolean = true
    final override def isTests: Boolean = true

  case object Suite extends One(SuiteSelector()) with SuiteLike:
    override def suiteId : Option[String] = None
    override def testName: Option[String] = None

  final case class NestedSuite(override val selector: NestedSuiteSelector) extends One(selector) with SuiteLike:
    override def suiteId : Option[String] = Option(selector.suiteId)
    override def testName: Option[String] = None

  final case class Test(override val selector: TestSelector) extends One(selector) with TestLike:
    override def suiteId : Option[String] = None
    override def testName: Option[String] = Option(selector.testName)

  final case class NestedTest(override val selector: NestedTestSelector) extends One(selector) with TestLike:
    override def suiteId : Option[String] = Option(selector.suiteId)
    override def testName: Option[String] = Option(selector.testName)

  def forTestClassRun(testClassRun: TestClassRun): Running =
    if testClassRun.testNames.length == 0 && testClassRun.testWildCards.length == 0
    then Suite
    else Tests(arrayConcat[Selector](
      arrayMap(testClassRun.testNames    , TestSelector        (_)),
      arrayMap(testClassRun.testWildCards, TestWildcardSelector(_))
    ))
    
  def forEvent(event: Event): Running = event.selector match
    case testWildcardSelector: TestWildcardSelector => throw IllegalArgumentException(s"Illegal event selector $testWildcardSelector!")
    case testSelector        : TestSelector         => Test(testSelector)
    case nestedTestSelector  : NestedTestSelector   => NestedTest(nestedTestSelector)
    case _                   : SuiteSelector        => Suite
    case nestedSuiteSelector : NestedSuiteSelector  => NestedSuite(nestedSuiteSelector)
