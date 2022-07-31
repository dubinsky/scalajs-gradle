package org.podval.tools.test

import sbt.testing.Framework

final class FrameworkTest(
  framework: Framework
) extends SyntheticTest:
  override def getParentId: Object = RootTest.getId
  override def getName: String = s"${framework.name} tests"
