package org.podval.tools.testing

import org.podval.tools.build.ScalaVersion

class ScalaJSTutorialTest extends GroupingFunSpec:
  groupTest(
    features = Seq(Feature("ScalaJS Tutorial")),
    fixtures = Seq(ScalaJSTutorialScalaTestFixture),
    platforms = Seq(
      Platform(ScalaVersion.Scala3         .versionDefault, isScalaJS = true),
      Platform(ScalaVersion.Scala2.Scala213.versionDefault, isScalaJS = true),
      Platform(ScalaVersion.Scala2.Scala212.versionDefault, isScalaJS = true),
    ),
    groupByFeature = true,
    combinedFixtureNameOpt = None
  )
