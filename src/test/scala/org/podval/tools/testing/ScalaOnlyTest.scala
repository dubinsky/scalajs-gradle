package org.podval.tools.testing

import org.podval.tools.build.ScalaVersion

class ScalaOnlyTest extends GroupingFunSpec:
  describe("scala-only"):
    groupTest(
      features = Seq(ScalaOnlyFeature),
      fixtures = Seq(ScalaOnlyFixture),
      platforms = Seq(Platform(ScalaVersion.Scala3.versionDefault, isScalaJS = false)),
      groupByFeature = true,
      combinedFixtureNameOpt = None
    )
    //  testLogging {
    ////    events "started", "skipped", "failed", "passed", "standard_error", "standard_out"
    ////    events "failed", "standard_error", "standard_out"
    //  }
