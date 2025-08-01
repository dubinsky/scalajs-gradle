package org.podval.tools.test.testproject

import org.gradle.api.internal.tasks.testing.junit.result.{TestClassResult, TestMethodResult}
import org.gradle.api.tasks.testing.TestResult.ResultType
import org.podval.tools.backend.BackendPlugin
import org.podval.tools.build.{ScalaBackend, ScalaBinaryVersion, ScalaLibrary, ScalaVersion}
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
  // but now the focus is on the mixed projects.
  protected def testByBackend: Boolean = false
  
  protected def groupByFeature: Boolean = true
  protected def testTaskMore: Seq[String] = Seq.empty
  protected def buildGradleFragments: Seq[String] = Seq.empty
  protected def checkRun: Boolean = false
  protected def features: Seq[Feature]
  protected def fixtures: Seq[Fixture]
  protected def scalaVersions: Seq[ScalaVersion] = ScalaBinaryVersion.all.map(_.scalaVersionDefault)
  protected def backends: Set[ScalaBackend] = ScalaBackend.all
  
  final protected def groupTestByFixtureAndCombined(): Unit =
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
              describe(fixture.framework.description):
                forScalaVersions(
                  projectName = Seq(
                    feature.name,
                    fixture.framework.description
                  ),
                  feature,
                  Seq(fixture),
                )
      else
        for fixture: Fixture <- fixtures do
          describe(fixture.framework.description):
            for feature: Feature <- features do
              describe(feature.name):
                forScalaVersions(
                  projectName = Seq(
                    fixture.framework.description,
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
              forScalaVersions(
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
              forScalaVersions(
                projectName = Seq(
                  combinedFixtureName,
                  feature.name
                ),
                feature,
                fixtures
              )

  private def forScalaVersions(
    projectName: Seq[String],
    feature: Feature,
    fixtures: Seq[Fixture]
  ): Unit =
    for scalaVersion: ScalaVersion <- scalaVersions do
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
    backend: ScalaBackend,
    scalaVersion: ScalaVersion
  ): Seq[Fixture] = fixtures
    .filter(_.framework.isBackendSupported(backend, scalaVersion))

  private def forBackends(
    projectName: Seq[String],
    feature: Feature,
    fixtures: Seq[Fixture],
    scalaVersion: ScalaVersion
  ): Unit =
    val backendsSupported: Set[ScalaBackend] = backends.filter(fixturesSupported(fixtures, _, scalaVersion).nonEmpty)

    if backendsSupported.nonEmpty then
      if backendsSupported.size == 1 || testByBackend then
        for backend: ScalaBackend <- backendsSupported do
          val fixturesSupported: Seq[Fixture] = this.fixturesSupported(fixtures, backend, scalaVersion)
          if fixturesSupported.nonEmpty then
            val backendString: String = s"on ${backend.name}"
            describe(backendString):
              forProject(
                projectName :+ backendString,
                feature,
                fixturesSupported,
                scalaVersion,
                backend
              )

      if backendsSupported.size > 1 then
        val backendString: String = "mixed"
        describe(backendString):
          forProject(
            projectName :+ backendString,
            feature,
            fixtures,
            scalaVersion,
            backendsSupported
          )

  private def writeProject(
    project: TestProject,
    scalaVersion: ScalaVersion,
    settingsFragments: Seq[String],
    testImplementation: Seq[String],
    buildFragments: Seq[String]
  ): Unit =
    val writer: TestProjectWriter = project.writer(backend = None)
    
    writer.writeSettings(Seq(
      Fragments.settingsManagement,
      Fragments.rootProjectName(project.projectNameString),
      Fragments.includeScalaJsPluginBuild,
    ) ++ settingsFragments)
    
    writer.writeBuild(
      Seq(
        Fragments.applyScalaJsPlugin,
        Fragments.continueOnFailure,
        Fragments.noJava,
        Fragments.scalaVersion(scalaVersion),
        Fragments.dependencies(
          implementation = Seq.empty,
          testImplementation = testImplementation
        )
      ) ++ buildFragments ++ buildGradleFragments
    )
  
  private def forProject(
    projectName: Seq[String],
    feature: Feature,
    fixtures: Seq[Fixture],
    scalaVersion: ScalaVersion,
    backend: ScalaBackend
  ): Unit =
    def createProject: TestProject =
      val project: TestProject = TestProject(projectName)
      writeProject(
        project,
        scalaVersion,
        settingsFragments = Seq.empty,
        testImplementation = frameworkDependencies(fixtures, scalaVersion, backend),
        buildFragments = Seq(testTask(feature, fixtures))
      )
      val writer: TestProjectWriter = project.writer(backend = None)
      writer.writeProperties(Seq(BackendPlugin.scalaBackendProperty -> backend.name))
      writer.writeSources(fixtures)
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
    scalaVersion: ScalaVersion,
    backends: Set[ScalaBackend]
  ): Unit =
    def createProject: TestProject =
      val project: TestProject = TestProject(projectName)
      writeProject(
        project,
        scalaVersion,
        settingsFragments = Seq(Fragments.includeSubprojects(backends.toSeq.map(_.sourceRoot))),
        testImplementation = Seq.empty,
        buildFragments = Seq.empty
      )
      for backend: ScalaBackend <- backends do
        val writer: TestProjectWriter = project.writer(backend = Some(backend))
        val fixturesSupported: Seq[Fixture] = this.fixturesSupported(fixtures, backend, scalaVersion)
        writer.writeBuild(Seq(
          Fragments.dependencies(
            implementation = Seq.empty,
            testImplementation = frameworkDependencies(fixturesSupported, scalaVersion, backend)
          ),
          testTask(feature, fixtures)
        ))
        writer.writeSources(fixturesSupported)
      project

    val project: Memo[TestProject] = Memo(createProject)
    val testResultsRetriever: Memo[TestResultsRetriever] = project.map(_.test(fixtures.flatMap(_.commandLineIncludeTestNames)))

    for backend: ScalaBackend <- backends do test(
      testResultsRetriever.map(_.testResults(backend = Some(backend))),
      s"${backend.sourceRoot} tests",
      checks = fixturesSupported(fixtures, backend, scalaVersion).flatMap(_.checks(feature))
    )
  
  private def frameworkDependencies(
    fixtures: Seq[Fixture],
    scalaVersion: ScalaVersion,
    backend: ScalaBackend
  ): Seq[String] = fixtures.map(_
    .framework
    .withBackend(backend)
    .dependencyNotation(
      backend,
      scalaLibrary = ScalaLibrary.fromScalaVersion(scalaVersion),
      version = None
    )
  )
  
  private def testTask(
    feature: Feature,
    fixtures: Seq[Fixture]
  ): String = Fragments.testTask(
    includeTestNames = fixtures.flatMap(_.includeTestNames),
    excludeTestNames = fixtures.flatMap(_.excludeTestNames),
    includeTags = feature.includeTags,
    excludeTags = feature.excludeTags,
    maxParallelForks = feature.maxParallelForks,
    more = testTaskMore
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
