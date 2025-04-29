package org.podval.tools.test.nested

import org.podval.tools.build.{ScalaBackendKind, ScalaPlatform, ScalaVersion}
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
    ScalaCheckFixture,
    ScalaTestFixture,
    UTestFixture,
    ZioTestFixture
    // does not support nested suites
    //    JUnit4ScalaJSFixture,
    //    MUnitFixture,
    //    Specs2Fixture,
  )

  val platforms: Seq[ScalaPlatform] = Seq(
    ScalaPlatform(ScalaVersion.Scala3         .versionDefault, ScalaBackendKind.JVM),
    ScalaPlatform(ScalaVersion.Scala3         .versionDefault, ScalaBackendKind.JS ),

    ScalaPlatform(ScalaVersion.Scala2.Scala213.versionDefault, ScalaBackendKind.JVM),
    ScalaPlatform(ScalaVersion.Scala2.Scala213.versionDefault, ScalaBackendKind.JS ),

    ScalaPlatform(ScalaVersion.Scala2.Scala212.versionDefault, ScalaBackendKind.JVM),
    ScalaPlatform(ScalaVersion.Scala2.Scala212.versionDefault, ScalaBackendKind.JS ),
  )
