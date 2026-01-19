package org.podval.tools.test.taskdef

import org.gradle.api.internal.tasks.testing.{DefaultTestClassDescriptor, DefaultTestMethodDescriptor,
  TestDescriptorInternal}
import org.podval.tools.platform.Scala212Collections.arrayConcat
import sbt.testing.{NestedSuiteSelector, NestedTestSelector, Selector, SuiteSelector, Task, TestSelector,
  TestWildcardSelector}

// Wrapper around Selector.
sealed trait Selectors:
  def selector: Selector
  def selectors: Array[Selector]
  def isSuite: Boolean
  def isTest : Boolean
  def isTests: Boolean
  def testDescriptor(testId: AnyRef, className: String): TestDescriptorInternal

  final def sameAs(that: Selectors): Boolean = Selectors.selectorsEqual(this.selector, that.selector)

  // TODO move out?
  final def forNestedTask(nestedTask: Task): Selectors =
    require(isSuite, s"$selector can not have nested tests!")

    val nestedSelectors: Array[Selector] = nestedTask.taskDef.selectors
    require(nestedSelectors.length == 1, "Only one selector can be nested!")

    nestedSelectors(0) match
      case nestedSuiteSelector: NestedSuiteSelector => Selectors.NestedSuite(nestedSuiteSelector)
      case nestedTestSelector : NestedTestSelector  => Selectors.NestedTest(nestedTestSelector)
      case testSelector       : TestSelector        => Selectors.Test(testSelector) // ScalaCheck does this ;)
      case selector => throw IllegalArgumentException(s"$selector can not be nested")

object Selectors:
  final case class Tests(
    testSelectors: Array[TestSelector],
    testWildcardSelectors: Array[TestWildcardSelector]
  ) extends Selectors:
    override def selector = SuiteSelector()
    override def selectors: Array[Selector] = arrayConcat(testSelectors, testWildcardSelectors)
    override def isSuite: Boolean = true
    override def isTest : Boolean = false
    override def isTests: Boolean = true
    override def testDescriptor(testId: AnyRef, className: String): TestDescriptorInternal =
      DefaultTestClassDescriptor(testId, className)

  sealed abstract class One(override val selector: Selector) extends Selectors:
    final override def selectors: Array[Selector] = Array(selector)

  sealed trait SuiteLike extends Selectors:
    final override def isSuite: Boolean = true
    final override def isTest : Boolean = false
    final override def isTests: Boolean = false

  sealed trait TestLike extends Selectors:
    final override def isSuite: Boolean = false
    final override def isTest : Boolean = true
    final override def isTests: Boolean = true

  case object Suite extends One(SuiteSelector()) with SuiteLike:
    override def testDescriptor(testId: AnyRef, className: String): TestDescriptorInternal =
      DefaultTestClassDescriptor(testId, className)

  final case class NestedSuite(override val selector: NestedSuiteSelector) extends One(selector) with SuiteLike:
    override def testDescriptor(testId: AnyRef, className: String): TestDescriptorInternal =
      DefaultTestClassDescriptor(testId, selector.suiteId) // TODO ?

  final case class Test(override val selector: TestSelector) extends One(selector) with TestLike:
    override def testDescriptor(testId: AnyRef, className: String): TestDescriptorInternal =
      DefaultTestMethodDescriptor(testId, className, selector.testName)

  final case class NestedTest(override val selector: NestedTestSelector) extends One(selector) with TestLike:
    override def testDescriptor(testId: AnyRef, className: String): TestDescriptorInternal =
      DefaultTestMethodDescriptor(testId, selector.suiteId, selector.testName)

  def apply(selector: Selector): Selectors = selector match
    case testWildcardSelector: TestWildcardSelector => throw IllegalArgumentException(s"Illegal event selector $testWildcardSelector!")
    case testSelector        : TestSelector         => Test(testSelector)
    case nestedTestSelector  : NestedTestSelector   => NestedTest(nestedTestSelector)
    case _                   : SuiteSelector        => Suite
    case nestedSuiteSelector : NestedSuiteSelector  => NestedSuite(nestedSuiteSelector)

  // Although `Selector` subclasses do implement `equals()`, it does not seem to work for some reason:
  // TestSelector(org.podval.tools.test.JUnit4Test.assumeFalse) != TestSelector(org.podval.tools.test.JUnit4Test.assumeFalse)
  def selectorsEqual(left: Selector, right: Selector): Boolean = (left, right) match
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
