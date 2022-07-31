package org.podval.tools.test

import sbt.testing.Framework

object RootTest extends SyntheticTest:
  override def getParentId: Object = null
  override def getName: String = "SBT Tests"

  def forFramework(framework: Framework, groupByFramework: Boolean): SyntheticTest =
    if groupByFramework then forFramework(framework) else this

  def forFramework(framework: Framework): FrameworkTest = FrameworkTest(
    framework
  )
