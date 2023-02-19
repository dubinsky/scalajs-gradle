package org.podval.tools.test

import org.gradle.api.internal.tasks.testing.{DefaultTestClassDescriptor, DefaultTestMethodDescriptor, TestClassRunInfo,
  TestDescriptorInternal}
import org.podval.tools.test.serializer.{FrameworkSerializer, TaskDefSerializer}
import sbt.testing.{Framework, NestedTestSelector, Selector, TaskDef, TestSelector, TestWildcardSelector}

// Note: in reality, an individual test is not always a method (e.g., in ScalaTest), but compared to a class it is :)
final class TaskDefTest(
  val id: AnyRef,
  val framework: Framework,
  val taskDef: TaskDef
) extends TestClassRunInfo:

  override def getTestClassName: String = taskDef.fullyQualifiedName

  def isComposite: Boolean = methodName.isEmpty

  def methodName: Option[String] =
    if taskDef.selectors.length != 1 then None else TaskDefTest.methodName(taskDef.selectors.head)

  def verifyCanHaveNestedTest(taskDef: TaskDef): Unit =
    require(isComposite, "Method tests can not have nested tests")
    val selectors: Array[Selector] = taskDef.selectors
    require(selectors.nonEmpty, s"No selectors in nested TaskDef: $taskDef")
    require(selectors.length == 1, s"More than one selector in nested TaskDef: TaskDef")
    require(!selectors.head.isInstanceOf[TestWildcardSelector], "Encountered TestWildcardSelector!")

  override def toString: String = toTestDescriptorInternal.toString

  def toTestDescriptorInternal: TestDescriptorInternal =
    if isComposite
    then DefaultTestClassDescriptor (id, getTestClassName)
    else DefaultTestMethodDescriptor(id, getTestClassName, methodName.get)

  // TODO do I need this?
  private given CanEqual[AnyRef, AnyRef] = CanEqual.derived
  override def equals(obj: Any): Boolean = obj.isInstanceOf[TaskDefTest] && {
    val other: TaskDefTest = obj.asInstanceOf[TaskDefTest]

    id == other.id &&
    FrameworkSerializer.equal(framework, other.framework) &&
    TaskDefSerializer.equal(taskDef, other.taskDef)
  }

object TaskDefTest:
  def methodName(selector: Selector): Option[String] = selector match
    case testSelector: TestSelector => Some(testSelector.testName)
    case nestedTestSelector: NestedTestSelector => Some(nestedTestSelector.testName)
    case _ => None
