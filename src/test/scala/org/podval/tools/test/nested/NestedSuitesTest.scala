package org.podval.tools.test.nested

import org.podval.tools.test.testproject.{Feature, Fixture, GroupingFunSpec}

class NestedSuitesTest extends GroupingFunSpec:
  groupTestByFixtureAndCombined()
  
  override protected def features: Seq[Feature] = List(
    NestedSuitesTest.nestedSuites,
  )
  
  override protected def fixtures: List[Fixture] = List(
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

object NestedSuitesTest:
  val nestedSuites: Feature = Feature(
    name = "nested suites"
  )
