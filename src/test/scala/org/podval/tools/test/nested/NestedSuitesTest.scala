package org.podval.tools.test.nested

import org.podval.tools.test.testproject.{Feature, Fixture, GroupingFunSpec}

class NestedSuitesTest extends GroupingFunSpec:
  groupTestByFixtureAndCombined()
  
  override protected def features: Seq[Feature] = List(
    NestedSuitesTest.nestedSuites,
  )
  
  override protected def fixtures: List[Fixture] = List(
    // only for frameworks that support nested suited
    JUnit4Fixture,
    ScalaCheckFixture,
    ScalaTestFixture,
    UTestFixture,
    ZioTestFixture
  )

object NestedSuitesTest:
  val nestedSuites: Feature = Feature(
    name = "nested suites"
  )
