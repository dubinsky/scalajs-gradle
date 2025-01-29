package org.podval.tools.testing

import org.podval.tools.build.ScalaLibraryDependency

class ScalaJSTutorialTest extends GroupingFunSpec:
  groupTest(
    features = Seq(Feature("ScalaJS Tutorial")),
    fixtures = Seq(ScalaJSTutorialScalaTestFixture),
    platforms = Seq(
      Platform(ScalaLibraryDependency.Scala3.versionDefault  , isScalaJS = true),
      Platform(ScalaLibraryDependency.Scala2.versionDefault13, isScalaJS = true),
      Platform(ScalaLibraryDependency.Scala2.versionDefault12, isScalaJS = true),
    ),
    groupByFeature = true,
    combinedFixtureNameOpt = None
  )
