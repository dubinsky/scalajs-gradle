package org.podval.tools.test.testproject

import org.podval.tools.test.framework.FrameworkDescriptor

abstract class Fixture(
  val framework: FrameworkDescriptor,
  val mainSources: Seq[SourceFile] = Seq.empty,
  val testSources: Seq[SourceFile],
  val runOutputExpectations: Seq[String] = Seq.empty
):
  def checks(feature: Feature): Seq[ForClass]
