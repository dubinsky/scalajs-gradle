package org.podval.tools.testing.framework

import org.podval.tools.build.{ScalaBackend, ScalaPlatform, ScalaVersion}
import org.podval.tools.testing.testproject.{Feature, Fixture, GroupingFunSpec}

// TODO when run separately, this class produces correctly nested results;
// when it is run together with other tests, something (Idea?) flattens the results...
// Or are the *events* different?
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
    excludeTags = Seq("org.podval.tools.testing.ExcludedTest")
  )
  
  val features: List[Feature] = List(
    basicFunctionality
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
    ScalaPlatform(ScalaVersion.Scala3.versionDefault, ScalaBackend.Jvm),
    ScalaPlatform(ScalaVersion.Scala3.versionDefault, ScalaBackend.JS ()),
  )
