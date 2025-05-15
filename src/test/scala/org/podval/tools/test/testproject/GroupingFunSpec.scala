package org.podval.tools.test.testproject

import org.gradle.api.internal.tasks.testing.junit.result.{TestClassResult, TestMethodResult}
import org.gradle.api.tasks.testing.TestResult.ResultType
import org.podval.tools.build.{Dependency, ScalaBackendKind, ScalaPlatform, ScalaVersion, Version}
import org.podval.tools.test.framework.FrameworkDescriptor
import org.podval.tools.scalajsplugin.{GradleNames, ScalaJSPlugin}
import org.scalatest.funspec.AnyFunSpec

import scala.jdk.CollectionConverters.*
import ForClass.{ClassExpectation, MethodExpectation}
import ClassExpectation.*
import MethodExpectation.*

abstract class GroupingFunSpec extends AnyFunSpec:
  // In the past, running the tests for individual test frameworks in addition to the per-framework
  // helped uncover some dependency bugs; at this point, there is no much point in it, so we do not.
  protected def testByFixture: Boolean = false

  // Running tests per-backend in addition to mixed exercises mode-setting functionality,
  // but currently the focus is on the mixed projects, so no.
  protected def testByBackend: Boolean = true // TODO set to false once mixed projects work
  
  protected def groupByFeature: Boolean = true
  protected def buildGradleFragments: Seq[String] = Seq.empty
  protected def checkRun: Boolean = false
  protected def features: Seq[Feature]
  protected def fixtures: Seq[Fixture]
  protected def scalaVersions: Seq[Version] = ScalaVersion.versionDefaults
  protected def backends: Set[ScalaBackendKind] = ScalaBackendKind.all
  
  // TODO run it from the base class itself
//  groupTestByFixtureAndCombined()
  
  final protected def groupTestByFixtureAndCombined(): Unit =
    println(s"----- groupTestByFixtureAndCombined")
    if testByFixture || fixtures.size == 1 then
      groupTest()

    if fixtures.size > 1 then
      groupTest(combinedFixtureNameOpt = Some("combined"))

  final def groupTest(combinedFixtureNameOpt: Option[String] = None): Unit =
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
                  Seq(fixture)
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
                fixtures
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
                fixtures
              )

  private def forPlatforms(
    projectName: Seq[String],
    feature: Feature,
    fixtures: Seq[Fixture]
  ): Unit =
    for scalaVersion: Version <- scalaVersions do
      val scalaVersionString: String = s"in Scala v$scalaVersion"
      describe(scalaVersionString):
        forBackends(
          projectName :+ scalaVersionString,
          feature,
          fixtures,
          scalaVersion
        )

  private def fixturesSupported(
    fixtures: Seq[Fixture],
    backend: ScalaBackendKind
  ): Seq[Fixture] = fixtures
    .filter(_.framework.forBackend(backend).isSupported)

  private def forBackends(
    projectName: Seq[String],
    feature: Feature,
    fixtures: Seq[Fixture],
    scalaVersion: Version
  ): Unit =
    val backendsSupported: Set[ScalaBackendKind] = backends.filter(fixturesSupported(fixtures, _).nonEmpty)

    if backendsSupported.nonEmpty then
      if backendsSupported.size == 1 || testByBackend then
        for backend: ScalaBackendKind <- backendsSupported do
          val fixturesSupported: Seq[Fixture] = this.fixturesSupported(fixtures, backend)
          if fixturesSupported.nonEmpty then
            val backendString: String = s"on ${backend.displayName}"
            describe(backendString):
              forProject(
                projectName :+ backendString,
                feature,
                fixturesSupported,
                scalaVersion,
                backend
              )

      // TODO enable once I figure out the absent/number of tests/number of failed tests
      if backendsSupported.size > 1 then
        val backendString: String = "mixed"
//        describe(backendString):
//          forProject(
//            projectName :+ backendString,
//            feature,
//            fixtures,
//            scalaVersion,
//            backendsSupported
//          )

  private def forProject(
    projectName: Seq[String],
    feature: Feature,
    fixtures: Seq[Fixture],
    scalaVersion: Version,
    backend: ScalaBackendKind
  ): Unit =
    def createProject: TestProject =
      val project: TestProject = TestProject.writeProject(
        projectName,
        properties = Seq(ScalaJSPlugin.backendProperty -> backend.toString),
        dependencies = Map(
          "implementation" -> Seq(scalaDependency(scalaVersion)),
          "testImplementation" -> frameworkDependencies(fixtures, scalaVersion, backend)
        ),
        buildGradleFragments = this.buildGradleFragments ++ Seq(testTask(feature, fixtures))
      )

      project.writeSources(backend = None, isTest = false, fixtures.flatMap(_.mainSources))
      project.writeSources(backend = None, isTest = true , fixtures.flatMap(_.testSources))

      project

    val project: Memo[TestProject] = Memo(createProject)
    val testResultsRetriever: Memo[TestResultsRetriever] = project.map(_.test(fixtures.flatMap(_.commandLineIncludeTestNames)))

    test(
      testResults = testResultsRetriever.map(_.testResults(backend = None)),
      description = "tests",
      checks = fixtures.flatMap(_.checks(feature))
    )

    if checkRun then run(
      project,
      runOutputExpectations = fixtures.head.runOutputExpectations
    )

  private def forProject(
    projectName: Seq[String],
    feature: Feature,
    fixtures: Seq[Fixture],
    scalaVersion: Version,
    backends: Set[ScalaBackendKind]
  ): Unit =
    def createProject: TestProject =
      val testImplementationDependencies: Map[String, Seq[Dependency.WithVersion]] = backends
        .map((backend: ScalaBackendKind) =>
          GradleNames.testImplementationConfigurationName(backend) ->
            frameworkDependencies(fixturesSupported(fixtures, backend), scalaVersion, backend)
        )
        .toMap

      val project: TestProject = TestProject.writeProject(
        projectName,
        properties = Seq.empty,
        dependencies = Map("implementation" -> Seq(scalaDependency(scalaVersion))) ++ testImplementationDependencies,
        buildGradleFragments = this.buildGradleFragments // TODO name of the test task is backend-specific ++ Seq(testTask(feature, fixtures))
      )

      for backend: ScalaBackendKind <- backends do
        project.writeSources(backend = Some(backend), isTest = false, fixturesSupported(fixtures, backend).flatMap(_.mainSources))
        project.writeSources(backend = Some(backend), isTest = true , fixturesSupported(fixtures, backend).flatMap(_.testSources))

      project

    val project: Memo[TestProject] = Memo(createProject)
    val testResultsRetriever: Memo[TestResultsRetriever] = project.map(_.test(fixtures.flatMap(_.commandLineIncludeTestNames)))

    for backend: ScalaBackendKind <- backends do test(
      // TODO to obtain test results for all backends, I need to stop Gradle failing the build when tests for one fail...
      testResultsRetriever.map(_.testResults(Some(backend))),
      s"${backend.sourceRoot} tests",
      checks = fixturesSupported(fixtures, backend).flatMap(_.checks(feature))
    )

//    if doRun then run(
//      project,
//      runOutputExpectations = fixtures.head.runOutputExpectations
//    )

  private def scalaDependency(scalaVersion: Version) = ScalaVersion
    .forVersion(scalaVersion)
    .scalaLibraryDependency
    .withVersion(scalaVersion)
  
  private def frameworkDependencies(
    fixtures: Seq[Fixture],
    scalaVersion: Version,
    backend: ScalaBackendKind
  ): Seq[Dependency.WithVersion] =
    val platform: ScalaPlatform = ScalaPlatform(scalaVersion, backend)
    fixtures
      .map(_.framework)
      .map((framework: FrameworkDescriptor) =>
        require(framework.forBackend(backend).isSupported)
        framework.dependency(platform).withVersion(
          if platform.version.isScala3
          then framework.versionDefault
          else framework.versionDefaultScala2.getOrElse(framework.versionDefault)
        )
      )
    
  private def testTask(
    feature: Feature,
    fixtures: Seq[Fixture]
  ): String = TestTask.testTask(
    includeTestNames = fixtures.flatMap(_.includeTestNames),
    excludeTestNames = fixtures.flatMap(_.excludeTestNames),
    includeTags = feature.includeTags,
    excludeTags = feature.excludeTags,
    maxParallelForks = feature.maxParallelForks,
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
    testResults: Memo[List[TestClassResult]],
    description: String,
    checks: Seq[ForClass]
  ): Unit =
    describe(description):
      for forClass: ForClass <- checks do
        describe(s"class '${forClass.className}'"):
          checkClass(
            classExpectations = forClass.expectations,
            resultOptMemo = testResults
              .map(_.find(_.getClassName == s"${ForClass.testPackage}.${forClass.className}"))
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
