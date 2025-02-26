package org.podval.tools.test.scalajstutorial

import org.podval.tools.build.{ScalaBackend, ScalaPlatform, ScalaVersion}
import org.podval.tools.test.testproject.{Feature, GroupingFunSpec}

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
