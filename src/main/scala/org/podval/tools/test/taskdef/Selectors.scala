package org.podval.tools.test.taskdef

import org.gradle.api.internal.tasks.testing.{DefaultTestClassDescriptor, DefaultTestMethodDescriptor,
  TestDescriptorInternal}
import org.podval.tools.platform.Scala212Collections.{arrayConcat, arrayMap}
import sbt.testing.{NestedSuiteSelector, NestedTestSelector, Selector, SuiteSelector, TestSelector, TestWildcardSelector}

// Wrapper around one or many Selectors.
sealed trait Selectors:
  def selector: Selector
  def selectors: Array[Selector]
  def isSuite: Boolean
  def isTests: Boolean
  def testDescriptor(testId: AnyRef, className: String): TestDescriptorInternal

object Selectors:
  sealed abstract class One(override val selector: Selector) extends Selectors:
    final override def selectors: Array[Selector] = Array(selector)

  sealed trait SuiteLike extends Selectors:
    final override def isSuite: Boolean = true
    final override def isTests: Boolean = false

  sealed trait TestLike extends Selectors:
    final override def isSuite: Boolean = false
    final override def isTests: Boolean = true

  case object Suite extends One(SuiteSelector()) with SuiteLike:
    override def testDescriptor(testId: AnyRef, className: String): TestDescriptorInternal =
      DefaultTestClassDescriptor(testId, className)

  final case class NestedSuite(override val selector: NestedSuiteSelector) extends One(selector) with SuiteLike:
    override def testDescriptor(testId: AnyRef, className: String): TestDescriptorInternal =
      DefaultTestClassDescriptor(testId, selector.suiteId)

  final case class Test(override val selector: TestSelector) extends One(selector) with TestLike:
    override def testDescriptor(testId: AnyRef, className: String): TestDescriptorInternal =
      DefaultTestMethodDescriptor(testId, className, selector.testName)

  final case class NestedTest(override val selector: NestedTestSelector) extends One(selector) with TestLike:
    override def testDescriptor(testId: AnyRef, className: String): TestDescriptorInternal =
      DefaultTestMethodDescriptor(testId, selector.suiteId, selector.testName)

  final case class Tests(override val selectors: Array[Selector]) extends Selectors:
    override def selector = SuiteSelector()
    override def isSuite: Boolean = true
    override def isTests: Boolean = true
    override def testDescriptor(testId: AnyRef, className: String): TestDescriptorInternal =
      DefaultTestClassDescriptor(testId, className)

  def apply(testNames: Array[String], testWildcards: Array[String]): Selectors =
    if testNames.length == 0 && testWildcards.length == 0
    then Selectors.Suite
    else Selectors.Tests(arrayConcat(
      arrayMap(testNames    , TestSelector        (_)),
      arrayMap(testWildcards, TestWildcardSelector(_))
    ))

  def apply(selector: Selector): Selectors = selector match
    case _                   : SuiteSelector        => Suite
    case testSelector        : TestSelector         => Test       (testSelector)
    case nestedSuiteSelector : NestedSuiteSelector  => NestedSuite(nestedSuiteSelector)
    case nestedTestSelector  : NestedTestSelector   => NestedTest (nestedTestSelector)
    case testWildcardSelector: TestWildcardSelector =>
      throw IllegalArgumentException(s"Illegal selector $testWildcardSelector!")

  // Although `Selector` subclasses do implement `equals()`, it does not seem to work for some reason:
  // TestSelector(org.podval.tools.test.JUnit4Test.assumeFalse) != TestSelector(org.podval.tools.test.JUnit4Test.assumeFalse)
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
