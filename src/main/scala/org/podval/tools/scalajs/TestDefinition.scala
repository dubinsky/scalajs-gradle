package org.podval.tools.scalajs

import org.gradle.api.internal.tasks.testing.TestDescriptorInternal
import org.gradle.internal.id.CompositeIdGenerator
import sbt.testing.TaskDef // SBT: test-interface

// Note: based on sbt.TestFramework from org.scala-sbt.testing
// Note can not extend sbt.testing.TaskDef because that one is final.
// TODO separate name from className
final class TestDefinition(val taskDef: TaskDef, override val isComposite: Boolean) extends TestDescriptorInternal:
  def name: String = taskDef.fullyQualifiedName

  override def getName: String = name
  override def getDisplayName: String = name
  override def getClassName: String = name
  override def getClassDisplayName: String = name
  override def getParent: TestDescriptorInternal = null
  override def getId: CompositeIdGenerator.CompositeId = null
