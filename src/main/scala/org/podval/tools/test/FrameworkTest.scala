package org.podval.tools.test

import sbt.testing.Framework

final class FrameworkTest(
  parentId: Object,
  framework: Framework
) extends SyntheticTest(
  parentId = parentId,
  id = FrameworkTest.id(framework),
  name = s"${framework.name} tests"
)

object FrameworkTest:
  def id(framework: Framework): Object = framework.name
