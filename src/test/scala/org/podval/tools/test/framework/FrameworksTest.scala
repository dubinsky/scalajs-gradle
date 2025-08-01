package org.podval.tools.test.framework

import org.podval.tools.build.ScalaBinaryVersion
import org.podval.tools.test.testproject.{Feature, Fixture, GroupingFunSpec}

class FrameworksTest extends GroupingFunSpec:
  groupTestByFixtureAndCombined()

  override protected def features: Seq[Feature] = List(
    FrameworksTest.basicFunctionality,
    FrameworksTest.withTagInclusions
  )

  override protected def fixtures: Seq[Fixture] = List(
    JUnit4JvmFixture,
    JUnit4ScalaJSFixture,
    JUnit4ScalaNativeFixture,
    AirSpecFixture,
    HedgehogFixture,
    MUnitFixture,
    ScalaCheckFixture,
    ScalapropsFixture,
    ScalaTestFixture,
    Specs2Fixture,
    UTestFixture,
    WeaverTestFixture,
    ZioTestFixture
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
