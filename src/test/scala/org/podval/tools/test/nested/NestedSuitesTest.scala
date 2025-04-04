package org.podval.tools.test.nested

import org.podval.tools.build.{ScalaBackend, ScalaPlatform, ScalaVersion}
import org.podval.tools.test.testproject.{Feature, Fixture, GroupingFunSpec}

class NestedSuitesTest extends GroupingFunSpec:
  groupTest(
    features = NestedSuitesTest.features,
    fixtures = NestedSuitesTest.fixtures,
    platforms = NestedSuitesTest.platforms
  )

  groupTest(
    features = NestedSuitesTest.features,
    fixtures = NestedSuitesTest.fixtures,
    platforms = NestedSuitesTest.platforms,
    combinedFixtureNameOpt = Some("combined frameworks")
  )

object NestedSuitesTest:
  val nestedSuites: Feature = Feature(
    name = "nested suites"
  )
  
  val features: List[Feature] = List(
    nestedSuites,
  )

  val fixtures: List[Fixture] = List(
    JUnit4Fixture,
//    JUnit4ScalaJSFixture, // does not support nested suites
//    MUnitFixture,
    ScalaCheckFixture,
    ScalaTestFixture,
//    Specs2Fixture,
//    UTestFixture,
//    ZioTestFixture
  )

  val platforms: Seq[ScalaPlatform] = Seq(
    ScalaPlatform(ScalaVersion.Scala3         .versionDefault, ScalaBackend.Jvm),
    ScalaPlatform(ScalaVersion.Scala3         .versionDefault, ScalaBackend.JS ()),

    ScalaPlatform(ScalaVersion.Scala2.Scala213.versionDefault, ScalaBackend.Jvm),
    ScalaPlatform(ScalaVersion.Scala2.Scala213.versionDefault, ScalaBackend.JS ()),

    ScalaPlatform(ScalaVersion.Scala2.Scala212.versionDefault, ScalaBackend.Jvm),
    ScalaPlatform(ScalaVersion.Scala2.Scala212.versionDefault, ScalaBackend.JS ()),
  )
