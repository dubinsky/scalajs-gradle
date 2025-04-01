package org.podval.tools.test.taskdef

import sbt.testing.Framework

final class FrameworkProviderNonForking(
  override val framework: Framework
) extends FrameworkProvider:
  override def frameworkName: String = framework.name
