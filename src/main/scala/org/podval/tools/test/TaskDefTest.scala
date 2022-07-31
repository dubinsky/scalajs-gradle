package org.podval.tools.test

import org.gradle.api.internal.tasks.testing.TestClassRunInfo
import org.gradle.internal.id.IdGenerator
import org.podval.tools.test.serializer.{FrameworkSerializer, TaskDefSerializer}
import sbt.testing.{Framework, NestedTestSelector, Selector, TaskDef, TestSelector, TestWildcardSelector}

// Note: in reality, an individual test is not always a method (e.g., in ScalaTest), but compared to a class it is :)
final class TaskDefTest(
  override val getParentId: Object,
  override val getId: Object,
  val framework: Framework,
  val taskDef: TaskDef
) extends Test with TestClassRunInfo:

  override def getTestClassName: String = getClassName
  override def getClassName: String = taskDef.fullyQualifiedName
  override def getName: String = methodName.getOrElse(getClassName)
  override def isComposite: Boolean = methodName.isEmpty

  override def toString: String =
    if isComposite
    then s"Suite $getClassName"
    else s"Test $getClassName.$getName"

  def methodName: Option[String] = if taskDef.selectors.length != 1 then None else taskDef.selectors.head match
    case testSelector      : TestSelector       => Some(testSelector      .testName)
    case nestedTestSelector: NestedTestSelector => Some(nestedTestSelector.testName)
    case _ => None

  def thisOrNested(
    taskDef: TaskDef,
    mustBeNested: Boolean,
    idGenerator: IdGenerator[?]
  ): TaskDefTest =

    val selectors: Array[Selector] = taskDef.selectors
    require(selectors.nonEmpty, s"No selectors in nested TaskDef: $taskDef")
    require(selectors.length == 1, s"More than one selector in nested TaskDef: TaskDef")
    require(!selectors.head.isInstanceOf[TestWildcardSelector], "Encountered TestWildcardSelector!")
    require(isComposite || !mustBeNested, "Method tests can not have nested tests")

    if TaskDefSerializer.equal(taskDef, this.taskDef) && !mustBeNested
    then this
    else TaskDefTest(
      getParentId = getId,
      getId = idGenerator.generateId,
      framework = framework,
      taskDef = taskDef
    )

  private given CanEqual[Object, Object] = CanEqual.derived

  override def equals(obj: Any): Boolean = obj.isInstanceOf[TaskDefTest] && {
    val other: TaskDefTest = obj.asInstanceOf[TaskDefTest]

    getParentId == other.getParentId &&
    getId == other.getId &&
    FrameworkSerializer.equal(framework, other.framework) &&
    TaskDefSerializer.equal(taskDef, other.taskDef)
  }
