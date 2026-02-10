package org.podval.tools.test.testproject

import org.podval.tools.build.TestFramework

abstract class Fixture(
  val framework: TestFramework,
  val mainSources: Seq[SourceFile] = Seq.empty,
  val testSources: Seq[SourceFile],
  val includeTestNames: Seq[String] = Seq.empty,
  val excludeTestNames: Seq[String] = Seq.empty,
  val commandLineIncludeTestNames: Seq[String] = Seq.empty,
  val runOutputExpectations: Seq[String] = Seq.empty
):
  def checks(feature: Feature): Seq[ForClass]
