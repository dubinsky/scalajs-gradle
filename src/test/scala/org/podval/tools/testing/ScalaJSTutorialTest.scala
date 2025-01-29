package org.podval.tools.testing

import org.podval.tools.build.ScalaLibrary

class ScalaJSTutorialTest extends GroupingFunSpec:
  groupTest(
    features = Seq(Feature("ScalaJS Tutorial")),
    fixtures = Seq(ScalaJSTutorialScalaTestFixture),
    platforms = Seq(
      Platform(ScalaLibrary.Scala3.versionDefault  , isScalaJS = true),
      Platform(ScalaLibrary.Scala2.versionDefault13, isScalaJS = true),
      Platform(ScalaLibrary.Scala2.versionDefault12, isScalaJS = true),
    ),
    groupByFeature = true,
    combinedFixtureNameOpt = None
  )
