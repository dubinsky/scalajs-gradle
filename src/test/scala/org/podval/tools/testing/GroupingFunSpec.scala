package org.podval.tools.testing

import org.gradle.api.internal.tasks.testing.junit.result.{TestClassResult, TestMethodResult}
import org.gradle.api.tasks.testing.TestResult.ResultType
import org.podval.tools.testing.framework.FrameworkDescriptor
import org.scalatest.funspec.AnyFunSpec
import scala.jdk.CollectionConverters.*
import ForClass.{ClassExpectation, MethodExpectation}
import ClassExpectation.*
import MethodExpectation.*

class GroupingFunSpec extends AnyFunSpec:
  final def groupTest(
    features: Seq[Feature],
    fixtures: Seq[Fixture],
    platforms: Seq[Platform],
    groupByFeature: Boolean,
    combinedFixtureNameOpt: Option[String]
  ): Unit =
    if combinedFixtureNameOpt.isEmpty then
      if groupByFeature then
        for feature: Feature <- features do
          describe(feature.name)(
            for fixture: Fixture <- fixtures do
              describe(fixture.framework.displayName)(
                forPlatforms(
                  projectName = Seq(
                    feature.name,
                    fixture.framework.displayName
                  ),
                  feature,
                  Seq(fixture),
                  platforms
                )
              )
          )
      else
        for fixture: Fixture <- fixtures do
          describe(fixture.framework.displayName)(
            for feature: Feature <- features do
              describe(feature.name)(
                forPlatforms(
                  projectName = Seq(
                    fixture.framework.displayName,
                    feature.name
                  ),
                  feature,
                  Seq(fixture),
                  platforms
                )
              )
          )
    else
      val combinedFixtureName: String = combinedFixtureNameOpt.get
      if groupByFeature then
        for feature: Feature <- features do
          describe(feature.name)(
            describe(combinedFixtureName)(
              forPlatforms(
                projectName = Seq(
                  feature.name,
                  combinedFixtureName
                ),
                feature,
                fixtures,
                platforms
              )
            )
          )
      else
        describe(combinedFixtureName)(
          for feature: Feature <- features do
            describe(feature.name)(
              forPlatforms(
                projectName = Seq(
                  combinedFixtureName,
                  feature.name
                ),
                feature,
                fixtures,
                platforms
              )
            )
        )

  private def forPlatforms(
    projectName: Seq[String],
    feature: Feature,
    fixtures: Seq[Fixture],
    platforms: Seq[Platform]
  ): Unit = for platform: Platform <- platforms do
    val fixturesSupported: Seq[Fixture] = fixtures
      .filter(supports(_, platform))
      .filter(_.supports(feature, platform))

    if fixturesSupported.nonEmpty then
      val fixturesEffective: Seq[Fixture] = fixturesSupported
        .filter(_.works(feature, platform))

      if fixturesEffective.isEmpty
      then ignore(s"doesn't work yet ${platform.displayName}")(())
      else describe(platform.displayName)(
        forProject(
          projectName :+ platform.displayName,
          feature,
          fixturesEffective,
          platform
        )
      )

  private def forProject(
    projectName: Seq[String],
    feature: Feature,
    fixtures: Seq[Fixture],
    platform: Platform
  ): Unit =
    val manyFixtures: Boolean = fixtures.length > 1

    val project: Memo[TestProject] = Memo(TestProject.writeProject(
      projectName,
      platform,
      mainSources = fixtures.flatMap(_.mainSources),
      testSources = fixtures.flatMap(_.testSources),
      frameworks = fixtures.map(_.framework),
      includeTestNames = fixtures.flatMap(feature.includeTestNames),
      excludeTestNames = fixtures.flatMap(feature.excludeTestNames),
      includeTags = fixtures.flatMap(feature.includeTags),
      excludeTags = fixtures.flatMap(feature.excludeTags),
      maxParallelForks = fixtures.map(feature.maxParallelForks).min,
      mainClassName = if manyFixtures then None else fixtures.head.mainSources.headOption.map(_.name)
    ))

    test(
      project,
      checks = fixtures.flatMap(feature.checks),
      commandLineIncludeTestNames = fixtures.flatMap(feature.commandLineIncludeTestNames)
    )

    if !manyFixtures then
      run(
        project,
        runOutputExpectations = fixtures.head.runOutputExpectations
      )

  private def supports(fixture: Fixture, platform: Platform): Boolean =
    val framework: FrameworkDescriptor = fixture.framework
    if platform.isScalaJS then framework.isScalaJSSupported else framework.isScalaSupported

  private def run(
    project: Memo[TestProject],
    runOutputExpectations: Seq[String]
  ): Unit = if runOutputExpectations.nonEmpty then
    describe("run output") {
      val runOutput: Memo[String] = project.map(_.run)
      for runOutputExpectation: String <- runOutputExpectations do
        it(s"contains '$runOutputExpectation'")(assert(runOutput.get.contains(runOutputExpectation)))
    }

  private def test(
    project: Memo[TestProject],
    checks: Seq[ForClass],
    commandLineIncludeTestNames: Seq[String]
  ): Unit =
    val results: Memo[List[TestClassResult]] = project.map(_.test(commandLineIncludeTestNames)._1)
    describe("tests")(
      for forClass: ForClass <- checks do
        describe(s"class '${forClass.className}'")(
          checkClass(
            classExpectations = forClass.expectations,
            resultOptMemo = results.map(_.find(_.getClassName == s"${ForClass.testingPackage}.${forClass.className}"))
          )
        )
    )

  private def checkClass(
    classExpectations: Seq[ClassExpectation],
    resultOptMemo: Memo[Option[TestClassResult]]
  ): Unit =
    def resultOpt: Option[TestClassResult] = resultOptMemo.get
    for classExpectation: ClassExpectation <- classExpectations do
      def get[A](opt: Option[A]): A = opt.getOrElse(fail(s"no results where there should be"))
      classExpectation match
        case AbsentClass  => it("class absent")(assert(resultOpt.isEmpty))
        case PresentClass => it("class present")(get(resultOpt))
        case FailedCount (count) => it("number of failed tests" )(assertResult(count)(get(resultOpt).getFailuresCount))
        case SkippedCount(count) => it("number of skipped tests")(assertResult(count)(get(resultOpt).getSkippedCount ))
        case Method(methodName, methodExpectation) =>
          def methodResultOpt: Option[TestMethodResult] = get(resultOpt).getResults.asScala.toList.find(_.getName == methodName)
          given CanEqual[ResultType, ResultType] = CanEqual.derived
          methodExpectation match
            case AbsentMethod  => it(s"method '$methodName' absent")(assert(methodResultOpt.isEmpty))
            case PresentMethod => it(s"method '$methodName' present")(get(methodResultOpt))
            case MethodResult(resultType) => it(s"method '$methodName' result")(assertResult(resultType)(get(methodResultOpt).getResultType))
