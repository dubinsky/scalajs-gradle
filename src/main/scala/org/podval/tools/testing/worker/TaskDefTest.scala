package org.podval.tools.testing.worker

import org.gradle.api.internal.tasks.testing.{DefaultTestClassDescriptor, DefaultTestMethodDescriptor, TestClassRunInfo,
  TestDescriptorInternal}
import sbt.testing.{Framework, NestedTestSelector, Selector, TaskDef, TestSelector}

// Note: in reality, an individual test is not always a method (e.g., in ScalaTest), but compared to a class it is :)
// TODO [nested] handle nested suits
// Note: for Scala tests, I might as well carry just the Framework name: only that gets serialized anyway,
// and the Framework gets instantiated in the TestWorker;
// *but* for ScalaJS Framework is retrieved from the Node side and can not be instantiated...
final class TaskDefTest(
  val id: AnyRef,
  val framework: Either[String, Framework],
  val taskDef: TaskDef
) extends TestClassRunInfo:

  override def getTestClassName: String = taskDef.fullyQualifiedName

  override def toString: String = toTestDescriptorInternal.toString

  def withId(id: AnyRef): TaskDefTest = TaskDefTest(id, framework, taskDef)

  def isComposite: Boolean = methodName.isEmpty

  private def methodName: Option[String] =
    if taskDef.selectors.length != 1 then None else TaskDefTest.methodName(taskDef.selectors.head)

  def toTestDescriptorInternal: TestDescriptorInternal =
    if isComposite
    then DefaultTestClassDescriptor(id, getTestClassName)
    else DefaultTestMethodDescriptor(id, getTestClassName, methodName.get)

object TaskDefTest:
  def methodName(selector: Selector): Option[String] = selector match
    case testSelector: TestSelector => Some(testSelector.testName)
    case nestedTestSelector: NestedTestSelector => Some(nestedTestSelector.testName)
    case _ => None
