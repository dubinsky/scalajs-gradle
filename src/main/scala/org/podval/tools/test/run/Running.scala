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

  // JUnit4 and its friends use TestSelector in place of NestedTestSelector
  // and stick the class name in front of the method name;
  // there is no chance to convince them to correct this ;)  
  final def reconstructNestedTestSelector: Running = selector match
    case testSelector: TestSelector =>
      val testName: String = testSelector.testName
      val lastDot: Int = testName.lastIndexOf('.')
      if lastDot == -1
      then this
      else Running.NestedTest(NestedTestSelector(
        testName.substring(0, lastDot),
        testName.substring(lastDot + 1)
      ))
    case _ => this

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
  // Contains only TestSelectors and TestWildcardSelectors by construction.
  private final case class Tests(override val selectors: Array[Selector]) extends Running:
    override def selector = SuiteSelector()
    override def isSuite: Boolean = true
    override def isTest : Boolean = false
    override def isTests: Boolean = true
    override def suiteId : Option[String] = None
    override def testName: Option[String] = None

  private sealed abstract class One(override val selector: Selector) extends Running:
    final override def selectors: Array[Selector] = Array(selector)

  private sealed trait SuiteLike extends Running:
    final override def isSuite: Boolean = true
    final override def isTest : Boolean = false
    final override def isTests: Boolean = false

  private sealed trait TestLike extends Running:
    final override def isSuite: Boolean = false
    final override def isTest : Boolean = true
    final override def isTests: Boolean = true

  private case object Suite extends One(SuiteSelector()) with SuiteLike:
    override def suiteId : Option[String] = None
    override def testName: Option[String] = None

  private final case class NestedSuite(override val selector: NestedSuiteSelector) extends One(selector) with SuiteLike:
    override def suiteId : Option[String] = Option(selector.suiteId)
    override def testName: Option[String] = None

  private final case class Test(override val selector: TestSelector) extends One(selector) with TestLike:
    override def suiteId : Option[String] = None
    override def testName: Option[String] = Option(selector.testName)

  private final case class NestedTest(override val selector: NestedTestSelector) extends One(selector) with TestLike:
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
