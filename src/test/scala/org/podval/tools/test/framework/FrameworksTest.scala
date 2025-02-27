package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaBackend, ScalaPlatform, ScalaVersion}
import org.podval.tools.test.testproject.{Feature, Fixture, GroupingFunSpec}

class FrameworksTest extends GroupingFunSpec:
  groupTest(
    features = FrameworksTest.features,
    fixtures = FrameworksTest.fixtures,
    platforms = FrameworksTest.platforms
  )

  groupTest(
    features = FrameworksTest.features,
    fixtures = FrameworksTest.fixtures,
    platforms = FrameworksTest.platforms,
    combinedFixtureNameOpt = Some("combined frameworks")
  )

object FrameworksTest:
  val basicFunctionality: Feature = Feature(
    name = "basic functionality",
    maxParallelForks = 2,
    excludeTags = Seq("org.podval.tools.test.ExcludedTest")
  )

  val withTagInclusions: Feature = Feature(
    name = "with tag inclusions",
    maxParallelForks = 2,
    includeTags = Seq("org.podval.tools.test.IncludedTest"),
    excludeTags = Seq("org.podval.tools.test.ExcludedTest")
  )
  
  val features: List[Feature] = List(
    basicFunctionality,
    withTagInclusions
  )

  val fixtures: List[Fixture] = List(
    JUnit4Fixture,
    JUnit4ScalaJSFixture,
    JUnit5Fixture,
    MUnitFixture,
    ScalaCheckFixture,
    ScalaTestFixture,
    Spec2Fixture,
    UTestFixture,
    ZioTestFixture
  )

  val platforms: Seq[ScalaPlatform] = Seq(
    ScalaPlatform(ScalaVersion.Scala3         .versionDefault, ScalaBackend.Jvm),
    ScalaPlatform(ScalaVersion.Scala3         .versionDefault, ScalaBackend.JS ()),

    ScalaPlatform(ScalaVersion.Scala2.Scala213.versionDefault, ScalaBackend.Jvm),
    ScalaPlatform(ScalaVersion.Scala2.Scala213.versionDefault, ScalaBackend.JS ()),

    ScalaPlatform(ScalaVersion.Scala2.Scala212.versionDefault, ScalaBackend.Jvm),
    // TODO this works - although Scala.js 1.0 does not support Scala 2.12?!
    ScalaPlatform(ScalaVersion.Scala2.Scala212.versionDefault, ScalaBackend.JS ()),
  )
