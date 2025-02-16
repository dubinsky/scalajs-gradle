package org.podval.tools.testing

import org.podval.tools.build.{ScalaBackend, ScalaPlatform, ScalaVersion}

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
