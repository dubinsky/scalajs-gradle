package org.podval.tools.testing

import org.podval.tools.build.{ScalaBackend, ScalaPlatform, ScalaVersion}

class ScalaOnlyTest extends GroupingFunSpec:
  describe("scala-only"):
    groupTest(
      features = Seq(ScalaOnlyFeature),
      fixtures = Seq(ScalaOnlyFixture),
      platforms = Seq(ScalaPlatform(ScalaVersion.Scala3.versionDefault, ScalaBackend.Jvm))
    )
    //  testLogging {
    ////    events "started", "skipped", "failed", "passed", "standard_error", "standard_out"
    ////    events "failed", "standard_error", "standard_out"
    //  }
