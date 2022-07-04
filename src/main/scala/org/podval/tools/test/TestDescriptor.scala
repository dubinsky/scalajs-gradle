package org.podval.tools.test

import org.gradle.api.internal.tasks.testing.TestDescriptorInternal
import sbt.testing.{Fingerprint, Selector, SuiteSelector, TaskDef, TestSelector}

// TODO set the parent and eliminate parentId?
sealed abstract class TestDescriptor(id: Object) extends TestDescriptorInternal:
  final override def getId: Object = id
  final override def getDisplayName: String = getName
  final override def getClassDisplayName: String = getClassName
  final override def getParent: TestDescriptor = null
  def getFullName: String

object TestDescriptor:
  sealed abstract class Suite(id: Object) extends TestDescriptor(id):
    final override def isComposite: Boolean = true

  final class Synthetic(
    id: Object,
    name: String
  ) extends Suite(id):
    override def getClassName: String = null
    override def getName: String = name
    override def toString: String = s"TestDescriptor.Synthetic($name)"
    override def getFullName: String = name

  sealed trait WithTaskDef(
    className: String,
    fingerprint: Fingerprint,
    explicitlySpecified: Boolean
  ) extends TestDescriptor:
    final override def getClassName: String = className
    def selector: Selector

    final def taskDef: TaskDef = TaskDef(
      className,
      fingerprint,
      explicitlySpecified,
      Array(selector)
    )

    final def withIndex(id: Object, index: Int): Class = new Class(
      id = id,
      className = className + "-" + index,
      fingerprint = fingerprint,
      explicitlySpecified = explicitlySpecified,
      includeMethods = Seq.empty
    )

  final class Class(
    id: Object,
    className: String,
    fingerprint: Fingerprint,
    explicitlySpecified: Boolean,
    val includeMethods: Seq[String]
  ) extends Suite(id) with WithTaskDef(
    className,
    fingerprint,
    explicitlySpecified
  ):
    override def getName: String = className
    override def getFullName: String = className
    override def toString: String = s"TestDescriptor.ClassWithTaskDef($getClassName)"
    override def selector = new SuiteSelector

    def withId(id: Object): Class = new Class(
      id = id,
      className = className,
      fingerprint = fingerprint,
      explicitlySpecified = explicitlySpecified,
      includeMethods = includeMethods
    )

    def method(
      id: Object,
      methodName: String
    ): Method = Method(
      id = id,
      className = className,
      methodName = methodName,
      fingerprint = fingerprint,
      explicitlySpecified = true
    )

  // Note: in reality, an individual test is not always a method (e.g., in ScalaTest), but compared to a class it is :)
  final class Method(
    id: Object,
    className: String,
    methodName: String,
    fingerprint: Fingerprint,
    explicitlySpecified: Boolean
  ) extends TestDescriptor(id) with WithTaskDef(
    className,
    fingerprint,
    explicitlySpecified
  ):
    override def isComposite: Boolean = false
    override def getName: String = methodName
    override def getFullName: String = s"$className.$methodName"
    override def toString: String = s"TestDescriptor.MethodWithTaskDef($getFullName)"
    override def selector = TestSelector(methodName)
