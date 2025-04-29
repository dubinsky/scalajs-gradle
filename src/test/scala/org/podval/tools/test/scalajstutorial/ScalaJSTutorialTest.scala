package org.podval.tools.test.scalajstutorial

import org.podval.tools.build.{ScalaBackendKind, ScalaPlatform, ScalaVersion}
import org.podval.tools.test.testproject.{Feature, GroupingFunSpec}

class ScalaJSTutorialTest extends GroupingFunSpec:
  groupTest(
    features = Seq(Feature("ScalaJS Tutorial")),
    fixtures = Seq(ScalaJSTutorialScalaTestFixture),
    platforms = Seq(
      ScalaPlatform(ScalaVersion.Scala3         .versionDefault, ScalaBackendKind.JS),
      ScalaPlatform(ScalaVersion.Scala2.Scala213.versionDefault, ScalaBackendKind.JS),
      ScalaPlatform(ScalaVersion.Scala2.Scala212.versionDefault, ScalaBackendKind.JS),
    )
  )
