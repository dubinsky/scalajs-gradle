package org.podval.tools.testing

import ForClass.*

class ScalaJSTutorialTest extends GroupingFunSpec:

  groupTest(
    features = Seq(Feature("ScalaJS Tutorial")),
    fixtures = Seq(ScalaJSTutorialScalaTestFixture),
    platforms = Seq(
      Platform(Platform.scala3VersionDefault  , isScalaJSDisabled = false),
      Platform(Platform.scala213VersionDefault, isScalaJSDisabled = false),
      Platform(Platform.scala212VersionDefault, isScalaJSDisabled = false),
    ),
    groupByFeature = true,
    combinedFixtureNameOpt = None
  )
