package org.podval.tools.test.framework

import org.podval.tools.build.{ScalaBackendKind, ScalaPlatform, ScalaVersion}
import org.podval.tools.test.testproject.{Feature, Fixture, GroupingFunSpec}

class FrameworksTest extends GroupingFunSpec:
  // In the past, running the tests for individual test frameworks helped uncover dome bugs;
  // at this point, there is no much point in it, so we do not:
//  groupTest(
//    features = FrameworksTest.features,
//    fixtures = FrameworksTest.fixtures,
//    platforms = FrameworksTest.platforms
//  )

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
    // TODO supply included tags on the build command line and reuse the projects?
    // TODO add tests for test case filtering?
    withTagInclusions
  )

  val fixtures: List[Fixture] = List(
    JUnit4Fixture,
    JUnit4ScalaJSFixture,
    JUnit4ScalaNativeFixture,
    JUnit5Fixture,
    MUnitFixture,
    ScalaCheckFixture,
    ScalaTestFixture,
    Specs2Fixture,
    UTestFixture,
    ZioTestFixture
  )

  val platforms: Seq[ScalaPlatform] = Seq(
    ScalaPlatform(ScalaVersion.Scala3         .versionDefault, ScalaBackendKind.JVM   ),
    ScalaPlatform(ScalaVersion.Scala3         .versionDefault, ScalaBackendKind.JS    ),
    ScalaPlatform(ScalaVersion.Scala3         .versionDefault, ScalaBackendKind.Native),

    ScalaPlatform(ScalaVersion.Scala2.Scala213.versionDefault, ScalaBackendKind.JVM   ),
    ScalaPlatform(ScalaVersion.Scala2.Scala213.versionDefault, ScalaBackendKind.JS    ),
    ScalaPlatform(ScalaVersion.Scala2.Scala213.versionDefault, ScalaBackendKind.Native),

    ScalaPlatform(ScalaVersion.Scala2.Scala212.versionDefault, ScalaBackendKind.JVM   ),
    ScalaPlatform(ScalaVersion.Scala2.Scala212.versionDefault, ScalaBackendKind.JS    ),
    ScalaPlatform(ScalaVersion.Scala2.Scala212.versionDefault, ScalaBackendKind.Native),
  )
