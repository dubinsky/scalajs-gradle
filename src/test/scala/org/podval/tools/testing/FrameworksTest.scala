package org.podval.tools.testing

// TODO when run separately, this class produces correctly nested results;
// when it is run together with other tests, something (Idea?) flattens the results...
// Or are the *events* different?
class FrameworksTest extends GroupingFunSpec:

  groupTest(
    features = FrameworksTest.features,
    fixtures = FrameworksTest.fixtures,
    platforms = FrameworksTest.platforms,
    groupByFeature = true,
    combinedFixtureNameOpt = None
  )

  groupTest(
    features = FrameworksTest.features,
    fixtures = FrameworksTest.fixtures,
    platforms = FrameworksTest.platforms,
    groupByFeature = true,
    combinedFixtureNameOpt = Some("combined frameworks")
  )

object FrameworksTest:
  val features: List[Feature] = List(
    Feature("basic functionality")
  )

  val fixtures: List[Fixture] = List(
    JUnit4Fixture,
    JUnit5Fixture,
    MUnitFixture,
    ScalaCheckFixture,
    ScalaTestFixture,
    Spec2Fixture,
    UTestFixture,
    ZioTestFixture
  )

  val platforms: Seq[Platform] = Seq(
    Platform(Platform.scala3VersionDefault, isScalaJSDisabled = true),
    Platform(Platform.scala3VersionDefault, isScalaJSDisabled = false),
  )
