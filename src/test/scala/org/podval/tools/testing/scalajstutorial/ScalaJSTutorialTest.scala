package org.podval.tools.testing.scalajstutorial

import org.podval.tools.build.{ScalaBackend, ScalaPlatform, ScalaVersion}
import org.podval.tools.testing.testproject.{Feature, GroupingFunSpec}

class ScalaJSTutorialTest extends GroupingFunSpec:
  groupTest(
    features = Seq(Feature("ScalaJS Tutorial")),
    fixtures = Seq(ScalaJSTutorialScalaTestFixture),
    platforms = Seq(
      ScalaPlatform(ScalaVersion.Scala3         .versionDefault, ScalaBackend.JS()),
      ScalaPlatform(ScalaVersion.Scala2.Scala213.versionDefault, ScalaBackend.JS()),
      ScalaPlatform(ScalaVersion.Scala2.Scala212.versionDefault, ScalaBackend.JS()),
    )
  )
