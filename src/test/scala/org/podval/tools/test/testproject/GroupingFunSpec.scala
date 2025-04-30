package org.podval.tools.test.testproject

import org.gradle.api.internal.tasks.testing.junit.result.{TestClassResult, TestMethodResult}
import org.gradle.api.tasks.testing.TestResult.ResultType
import org.podval.tools.build.{ScalaPlatform, Version}
import org.podval.tools.test.framework.FrameworkDescriptor
import org.scalatest.funspec.AnyFunSpec
import scala.jdk.CollectionConverters.*
import ForClass.{ClassExpectation, MethodExpectation}
import ClassExpectation.*
import MethodExpectation.*

class GroupingFunSpec extends AnyFunSpec:
  final def groupTest(
    features: Seq[Feature],
    fixtures: Seq[Fixture],
    platforms: Seq[ScalaPlatform],
    groupByFeature: Boolean = true,
    combinedFixtureNameOpt: Option[String] = None
  ): Unit =
    if combinedFixtureNameOpt.isEmpty then
      if groupByFeature then
        for feature: Feature <- features do
          describe(feature.name):
            for fixture: Fixture <- fixtures do
              describe(fixture.framework.displayName):
                forPlatforms(
                  projectName = Seq(
                    feature.name,
                    fixture.framework.displayName
                  ),
                  feature,
                  Seq(fixture),
                  platforms
                )
      else
        for fixture: Fixture <- fixtures do
          describe(fixture.framework.displayName):
            for feature: Feature <- features do
              describe(feature.name):
                forPlatforms(
                  projectName = Seq(
                    fixture.framework.displayName,
                    feature.name
                  ),
                  feature,
                  Seq(fixture),
                  platforms
                )
    else
      val combinedFixtureName: String = combinedFixtureNameOpt.get
      if groupByFeature then
        for feature: Feature <- features do
          describe(feature.name):
            describe(combinedFixtureName):
              forPlatforms(
                projectName = Seq(
                  feature.name,
                  combinedFixtureName
                ),
                feature,
                fixtures,
                platforms
              )
      else
        describe(combinedFixtureName):
          for feature: Feature <- features do
            describe(feature.name):
              forPlatforms(
                projectName = Seq(
                  combinedFixtureName,
                  feature.name
                ),
                feature,
                fixtures,
                platforms
              )

  private def forPlatforms(
    projectName: Seq[String],
    feature: Feature,
    fixtures: Seq[Fixture],
    platforms: Seq[ScalaPlatform]
  ): Unit = for platform: ScalaPlatform <- platforms do
    val fixturesSupported: Seq[Fixture] = fixtures
      .filter(_.framework.forBackend(platform.backendKind).isSupported)

    if fixturesSupported.nonEmpty then
      // TODO when we start running on different Node versions, use ScalaBackend to display Node version
      val platformDisplayName: String = s"in Scala v${platform.scalaVersion} on ${platform.backendKind.displayName}"
      describe(platformDisplayName):
        forProject(
          projectName :+ platformDisplayName,
          feature,
          fixturesSupported,
          platform
        )

  private def forProject(
    projectName: Seq[String],
    feature: Feature,
    fixtures: Seq[Fixture],
    platform: ScalaPlatform
  ): Unit =
    val manyFixtures: Boolean = fixtures.length > 1

    val project: Memo[TestProject] = Memo(TestProject.writeProject(
      projectName,
      platform,
      mainSources = fixtures.flatMap(_.mainSources),
      testSources = fixtures.flatMap(_.testSources),
      frameworks = fixtures.map(_.framework),
      includeTestNames = fixtures.flatMap(_.includeTestNames),
      excludeTestNames = fixtures.flatMap(_.excludeTestNames),
      includeTags = feature.includeTags,
      excludeTags = feature.excludeTags,
      maxParallelForks = feature.maxParallelForks,
      mainClassName = if manyFixtures then None else fixtures.head.mainSources.headOption.map(_.name)
    ))

    test(
      project,
      checks = fixtures.flatMap(_.checks(feature)),
      commandLineIncludeTestNames = fixtures.flatMap(_.commandLineIncludeTestNames)
    )

    if !manyFixtures then
      run(
        project,
        runOutputExpectations = fixtures.head.runOutputExpectations
      )

  private def run(
    project: Memo[TestProject],
    runOutputExpectations: Seq[String]
  ): Unit = if runOutputExpectations.nonEmpty then
    describe("run output"):
      val runOutput: Memo[String] = project.map(_.run)
      for runOutputExpectation: String <- runOutputExpectations do
        it(s"contains '$runOutputExpectation'")(assert(runOutput.get.contains(runOutputExpectation)))

  private def test(
    project: Memo[TestProject],
    checks: Seq[ForClass],
    commandLineIncludeTestNames: Seq[String]
  ): Unit =
    val results: Memo[List[TestClassResult]] = project.map(_.test(commandLineIncludeTestNames)._1)
    describe("tests"):
      for forClass: ForClass <- checks do
        describe(s"class '${forClass.className}'"):
          checkClass(
            classExpectations = forClass.expectations,
            resultOptMemo = results.map(_.find(_.getClassName == s"${ForClass.testPackage}.${forClass.className}"))
          )

  private def checkClass(
    classExpectations: Seq[ClassExpectation],
    resultOptMemo: Memo[Option[TestClassResult]]
  ): Unit =
    def resultOpt: Option[TestClassResult] = resultOptMemo.get
    for classExpectation: ClassExpectation <- classExpectations do
      def get[A](opt: Option[A]): A = opt.getOrElse(fail(s"no results where there should be"))
      classExpectation match
        case AbsentClass  => it("class absent" )(assert(resultOpt.isEmpty))
        case PresentClass => it("class present")(get(resultOpt))
        case TestCount   (count) => it("number of tests"        )(assertResult(count)(get(resultOpt).getTestsCount   ))
        case FailedCount (count) => it("number of failed tests" )(assertResult(count)(get(resultOpt).getFailuresCount))
        case SkippedCount(count) => it("number of skipped tests")(assertResult(count)(get(resultOpt).getSkippedCount ))
        case Method(methodName, methodExpectation) =>
          def methodResultOpt: Option[TestMethodResult] = get(resultOpt).getResults.asScala.toList.find(_.getName == methodName)
          given CanEqual[ResultType, ResultType] = CanEqual.derived
          methodExpectation match
            case AbsentMethod  => it(s"method '$methodName' absent" )(assert(methodResultOpt.isEmpty))
            case PresentMethod => it(s"method '$methodName' present")(get(methodResultOpt))
            case MethodResult(resultType) => it(s"method '$methodName' result")(assertResult(resultType)(get(methodResultOpt).getResultType))
