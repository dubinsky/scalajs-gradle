package org.podval.tools.testing

import org.podval.tools.testing.framework.FrameworkDescriptor

open class Fixture(
  final val framework: FrameworkDescriptor,
  final val mainSources: Seq[SourceFile] = Seq.empty,
  final val testSources: Seq[SourceFile],
  final val checks: Seq[ForClass],
  final val runOutputExpectations: Seq[String] = Seq.empty
):
  // TODO move
  def supports(feature: Feature, platform: Platform): Boolean = true
  def works   (feature: Feature, platform: Platform): Boolean = true
