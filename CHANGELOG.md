# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.4.8] - 2023-03-20
- feat: `node` extension
- feat: `node` and `npm` tasks
- feat: auto-install `NodeJS`
- feat: initialize Node project and install modules
- feat: `ZIO Test` support
- fix: running single test method needs to be wrapped in the implied suite
- fix: explicit `selectorsEqual()`
- fix: `FixUpRootTestOutputTestResultProcessor`
- test: tests for the assorted frameworks on ScalaJS
- cleanup: all ScalaJS code is in the `ScalaJS` subclasses
- cleanup: unfolded `TaskDefEx` into `TestClassProcessor`
- chore: latest `opentorah-util`

## [0.4.7] - 2023-03-13
- fix: do not specify success for the overall task completion in `TestClassProcessor.run()`
- fix: remove class name prefix from the test method names returned by `JUnit4` and `MUnit`
- fix: do not use `event.fullyQualifiedName` and `event.fingerprint`, lest Gradle interpret methods as classes
- feat: explicit `TestEnvironment.loadFrameworks(testClassPath)`
- feat: made running of the frameworks possible during test detection
- feat: made running Scala tests without forking possible
- cleanup: replaced `sbt.IO` calls
- cleanup: minimized classpath expansion
- cleanup: `TestClass` vs `TaskDefTestSpec`
- cleanup: dissolved `TaskDefTest`
- cleanup: updated to the latest Gradle's `DefaultTestExecuter`
- cleanup: processing test files one-by-one in the `TestFrameworkDetector`
- cleanup: delay some parameters of the `TestFramework`

## [0.4.6] - 2023-03-07
- build: automated test-projects tests
- fix: set `NODE_PATH` to point to `node_module` under the project root so that ScalaJS tests run
- feat: introduced `TestTask.useSbt` amd moved include/exclude tags into the `TestFrameworkOptions` closure
- cleanup: add `test-interface` to the `testImplementation` configuration when running plain Scala; then, there is no need to add the jar in the `TestFramework`'s `Action`
- cleanup: package structure
- cleanup: set test ids in the `TestClassProcessor`; use a placeholder for `rootTestSuiteId` and fix it up in `FixUpRootTestOutputTestResultProcessor`
- cleanup: switched from serializers to writers: deleted all serializers, serializer registry, `ForkingTestClassProcessor` and `TestWorker`
- cleanup: `AnalysisDetector`/`TestClass`
- cleanup: `sbt` configuration removed; `zinc` is used instead
- feat: ZIOTest
- feat: converting framework-specific exceptions to TestFailures
- chore: Gradle 8.0.2 and its DefaultTestExecuter

## [0.4.5] - 2023-02-26
- change: removed `groupByFramework` as not feasible
- chore: update ScalaJS DOM library
- fix: handle parentId for the nested tasks correctly (ScalaCheck uses them)
- cleanup: no TestStartEvent.parentId
- cleanup: no TaskDefTest.parentId
- cleanup: get rootTestSuiteId to the TestClassProcessor
- cleanup: all test ids are composite of any length, not just 2 as Gradle's serializer does
- cleanup: replicated all TestEventSerializer's serializers - but with my id serializer
- cleanup: no disambiguation needed in the TestSerializerRegistry
- cleanup: packaged proxying of the test events in the non-forking scenario into SingleThreddingTestResultProcessor
- cleanup: introduced TestFramework
- cleanup: packaged TestScanner as a TestFrameworkDetector
- cleanup: use Gradle's DefaultTest[Class|Method]Descriptor in test events instead of TaskDefTest
- cleanup: use Gradle's TestMainAction and WorkerTestClassProcessor
- cleanup: boiled down the differences between Gradle's ForkingTestClassProcessor and mine to overridable methods
- cleanup: boiled down the differences between Gradle's TestWorker and mine to overridable methods
- cleanup: boiled down the differences between Gradle's DafultTestExecuter and mine to overridable methods
- cleanup: tested with the modified Gradle

## [0.4.4] - 2023-02-19
- fix: "Could not dispatch message" caused by the presence of a test classes with no tests in them.cleanup: ForkingTestClassProcessor
- fix: tests fail to terminate when parallelized caused by the missing `TestFailureSerializer`
- feat: take Gradle log level into account when processing test output
- cleanup: consolidated test output processing in TestClassProcessor
- cleanup: factored out FrameworkRuns
- cleanup: removed TestResultProcessorEx
- chore: Gradle 8.0.1

## [0.4.3] - 2023-01-28
- chore: latest Gradle plugin publishing plugin
- chore: dependency updates

## [0.4.2] - 2022-12-19
- chore: Gradle 7.6 compatibility
- chore: update to the latest Scala, ScalaJS, ScalaTest, ScalaCheck, sbt

## [0.4.1] - 2022-09-15
- chore: dependency updates
- bug: fixed a Gradle issue with ModuleInitializers
- docs: documented NodeJS version compatibility

## [0.3.0] - 2022-08-14
- chore: cleanup
- feat: run Scala-only tests in parallel
- feat: added groupByFramework option to the test task
- feat: support ScalaCheck
- feat: support uTest
- feat: support specs2
- feat: support MUnit
- feat: support JUnit4
- build: use the plugin for plugin tests

Release theme: forking

To make serialization work, my own:
- TestSerializerRegistry
- ForkingTestClassProcessor
- WorkerTestClassProcessor
- TestWorker.

ScalaCheck *does* produce nested tasks - adjusted TestClassProcessor to handle them.

## [0.2.0] - 2022-07-31
- feat: Scala 2.12 support
- feat: report ignored tests
- wip: running tests in parallel
- refactor: package test functionality using Gradle test-related classes
- wip: test filtering
- refactor: use sbt Selectors in filtering tests
- wip: filtering suites based on tags
- test: move test projects into `test-projects`
- chore: latest Gradle, Zinc, opentorah, ScalaTest etc.

## [0.1.0] - 2022-07-18
- testing integrated with Gradle;
- testing integrated with IntelliJ Idea;
- running sbt test frameworks without ScalaJS;
- working on test filtering;
- latest Gradle, Zinc and opentorah;

**BREAKING CHANGES:**
- no more extension;
- task names changed;

## [0.0.4] - 2022-07-03
- ScalaJS tutorial for both Scala 2 and Scala 3;
- add correct dependencies and configuration for both Scala 3 and Scala 2;
- using zinc configured by the Scala plugin;
- using ScalaJS dependencies dynamically;
- working on Gradle-ifying test listening;
- source-map test failure messages;

## [0.0.3] - 2022-06-26
- serializing/deserializing linking report;
- verifying that the 'main' module exists before running and testing;
- moving towards using Gradle testing classes;
- add dependencies and Scala compiler options needed for ScalaJS;
 
## [0.0.2] - 2022-06-21
- running the linked code;
- running tests;

**BREAKING CHANGES:**
- task names changed
- configuration property `scalajs.outputDirectory` removed

Plugin:
- split plugin classes;
- stage configuration;
- distinct output directories for main/test, fast/full optimization;
- write linker report to disk;
- tasks for linking of the test code;
- depend on opentorah-util;
- depend on org.scala-js:scalajs-env-jsdom-nodejs for running on Node;
- tasks for running the JavaScript code on Node;
- dependencies and Gradle updated;

Testing ScalaJS:
- depend on org.scala-js:scalajs-sbt-test-adapter;
- depend on org.scala-sbt:test-interface;
- depend on org.scala-sbt:compiler-interface;
- depend on org.scala-sbt:zinc (persist, core, apiinfo);
- glue code inspired by a few classes from sbt (org.scala-sbt.testing, org.scala-sbt.actions);
- tasks for testing;

ScalaJS tutorial:
- added sbt-based project based on it;
- updated its dependencies;
- switched to Scala 3;
- switched to ScalaTest;
- added Gradle setup to it, using the plugin via composite build;
- depend on org.scala-js:scalajs-dom_sjs1;
- depend on org.scalatest:scalatest_sjs1 for tests;
- depend on org.scala-js:scalajs-test-bridge (something the plugin should add automatically);

## [0.0.1] - 2022-05-12
- first release;
- basic functionality (ScalaJS linker);
