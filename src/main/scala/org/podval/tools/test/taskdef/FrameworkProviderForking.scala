package org.podval.tools.test.taskdef

import sbt.testing.Framework

final class FrameworkProviderForking(
  override val frameworkName: String
) extends FrameworkProvider:
  override lazy val framework: Framework = frameworkDescriptor.newInstance.asInstanceOf[Framework]
