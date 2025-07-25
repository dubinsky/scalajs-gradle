# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.9.3] - 2025-07-21
- feat: `ZIO Test` on `Scala.js` and `Scala Native`;
- chore: dependency updates;
- cleanup;

## [0.9.2] - 2025-07-18
- feat: `specs2` on `ScalaNative`;
- chore: Gradle 9.0.0-rc-3;
- chore: dependency updates;
- cleanup;

## [0.9.1] - 2025-07-11
- chore: Gradle 9.0.0-rc-2;
- chore: dependency updates;
- cleanup;

## [0.9.0] - 2025-07-10
- chore: dependency updates;
- cleanup;

## [0.8.8] - 2025-07-07
- chore: dependency updates;
- cleanup;

## [0.8.7] - 2025-06-18
- chore: Gradle 9.0.0-rc-1;

## [0.8.6] - 2025-06-18
- cleanup: prepare for Gradle 9.0.0-rc-1;

## [0.8.5] - 2025-06-18
- cleanup;

## [0.8.4] - 2025-06-16
- cleanup;
- cleanup: `Version`;
- feat: support WASM for Scala.js;
- feat: select JS environment for Scala.js;
- fix: add Scala Parallel Collections Module dynamically;
- cleanup: `*Dependency*`;

## [0.8.3] - 2025-06-12
- cleanup: package `org.podval.tools.backend`;
- cleanup: package `org.podval.tools.scalaplugin` renamed to `org.podval.tools.backendplugin`;
- fix: project path in `settings-includes.gradle`;
- cleanup: shared sources;
- fix: shared resources;

## [0.8.2] - 2025-06-11
- doc: README update;
- chore: dependency updates;
- cleanup: `scalajsplugin` renamed to `scalaplugin`;
- cleanup: `ScalaJSPlugin` renamed to `BackendPlugin`;
- cleanup: `ScalaBackendExtension` renamed to `BackendExtension`;
- cleanup: removed `TestTask.backend`;
- feat: `BackendExtension` improvements;

## [0.8.1] - 2025-05-28
- feat: Scala version-specific sources;
- feat: Scala version property;
- feat: `ScalaBackendExtension`;
- fix: test interface goes to `testRuntimeOnly`, not `testImplementation` configuration;
- fix: `testScalaCompilerPlugins` configuration;
- fix: no `run` task on JVM;
- chore: dependency updates;
- cleanup: `ScalaVersion` and `ScalaBinaryVersion`; removed `ScalaPlatform`;
- cleanup: `Dependency.Maker` knows its `ScalaBackend`;
- cleanup: `OutputPiper.start(BaseExecSpec)` implemented;
- cleanup: use `abort` for `ScalaNative` too;
- cleanup: very few `Version`s can be `Compound`;
- doc: README update;

## [0.8.0] - 2025-05-23
- cleanup: `TestProject` and `TestProjectWriter`;
- feat: multiple backends in subprojects;
- doc: added usage hints;
- doc: multiple backends documentation;

## [0.7.9] - 2025-05-09 - unpublished
- doc: Scala Native;
- cleanup: use TaskProvider instead of Task more;
- cleanup: `NonJvm`;
- cleanup: `BackendDelegate`;
- cleanup: `GardleNames` and `BackendDelegateBinding`;
- cleanup: `GroupingFunSpec` and `TestProject`;
- cleanup: `scalajs` and `scalanative`;
- dependency updates;
- feat (WIP): multiple backends in the same project;

## [0.7.0] - 2025-05-05
- feat: support for Scala Native;
- cleanup: `BackendDelegate`;
- cleanup: `Version` (Simple, Compound);
- cleanup: `DependencyRequirement`;
- cleanup: `Named`;
- dependency updates;

## [0.6.3] - 2025-04-30
- cleanup: install the default version of Node.js if none is installed locally nor a version to install specified;
- cleanup: `RunTestClassProcessor`;
- cleanup: `FrameworkProvider`;
- cleanup: `ScalaBackend` and `ScalaBackendKind`;
- cleanup: `FrameworkDescriptor`;
- cleanup: `BackendDelegate` and `BackendDelegateKind`;
- doc: `README` improvements;
- dependency updates;
- feat (WIP): support Scala Native;

## [0.6.2] - 2025-04-06
- cleanup: `ScalaJSPlugin` and `BackendDelegate`s;
- cleanup: introduced `Running`;
- cleanup: introduced `FrameworkProvider`;
- cleanup: `RunTestClassProcessor`;
- dependency updates;
- doc: `README` improvements;

## [0.6.1] - 2025-03-31
- fix: running test cases of the nested test suites on ScalaTest;
- fix: attribute test cases to the correct test classes;
- test: added `NestedSuitesTest` for testing of the nested test suite support;
- doc: Nested Test Suites, Credits, etc.
- cleanup: `RunTestClassProcessor`;
- dependency updates;
- WIP: mixing JVM and Scala.js;

## [0.6.0] - 2025-03-25
- fix: remaining Scala 2.12 incompatibilities;
- feat: classfile-based test detection (including JUnit4 for Scala.js);
- cleanup: removed analysis-based test detection;
- feat: dry run support;
- cleanup: TODOs;
- cleanup: `test.filter`;
- cleanup: `test.environment`;
- cleanup: `scalajsplugin`;
- cleanup: `TestEnvironment`;
- cleanup: logging;
- cleanup: notes;
- build: GitHub CI workflows;
- doc: README notes cleanup;

## [0.5.1] - 2025-03-13
- feat: support Scala.js JUnit4 on Scala 2;
- feat: support filtering multiple specific tests in a class;
- doc: test filtering;
- cleanup: underlying test framework;
- cleanup: ScalaJSPlugin.Delegate;
- cleanup: test filtering;
- cleanup: test detection;
- cleanup: taskdef;
- cleanup: RunningTestClassProcessor;
- chore: dependency updates;

## [0.5.0] - 2025-02-28
- feat: support JUnit4 Scala.js framework;
- feat: test tagging for JUnit4;
- feat: test tagging for MUnit;
- feat: test tagging for ScalaTest;
- feat: test tagging for specs2;
- feat: test tagging for ZIO Test;
- feat: support test tag inclusions;
- feat: support JUnit4 assumptions;
- feat: running tests on Scala 2.13 JVM and JS;
- feat: running tests on Scala 2.12 JVM and JS;
- doc: documented test framework dependencies;
- doc: documented test framework test tagging;
- cleanup: ScalaOnlyTest re-absorbed and removed;
- cleanup: ScalaPlatform;
- cleanup: Dependency.Maker;
- cleanup: OptionStyle;
- cleanup: FrameworkDescriptor;
- cleanup: Scala.js actions;
- cleanup: braces;
- cleanup: documentation;
- cleanup: `testing` package (renamed `test`);
- chore: dependency updates;

## [0.4.16] - 2025-02-02
- chore: cleanup;
- chore: dependency updates;
- chore: Scala.js dependency update;
- chore: moved `build`, `node` and `platform` packages and some utility classes back here from OpenTorah: 
  the classes are only used here; if need be, I can package them into an artifact and publish them from here...
- cleanup: apply DependencyRequirement to configuration only, not to classpath, so not force resolution of the configurations!
- cleanup: split Scala.js dependencies;
- cleanup: add Scala.js dependencies to the plugin classpath in the plugin itself, not in the LinkTask;
- cleanup: add Zinc dependencies to the plugin classpath in the plugin itself, not in the TestTask;
- cleanup: no need to delay calculation of `analysisFile`;
- cleanup: ScalaJSTask.scalaJs;
- cleanup: set LinkTask.runtimeClassPath in the plugin to eliminate a `Task.getProject` call during task execution - the last such call!
- chore: Gradle update;
- cleanup: Node setup;
- cleanup: split the plugin class into `ScalaDelegate` and `ScalaJSDelegate`;
- cleanup: Gradle;
- doc: converted `README` from MarkDown to AsciiDoc;
- doc: refreshed `README`;
- doc: patch `README` with version attributes programmatically;
- build: set dependency versions from properties in `gradle.properties` written programmatically;
- build: `ScalaOnlyTest` now writes the `test-projects/scala-only` project instead of using prepared one;
- cleanup: org.podval.tools.testing.Sbt;
- cleanup: org.podval.tools.build.ScalaLibrary;
- cleanup: ScalaJS;
- cleanup: split out core Scala.js support;
- cleanup: *Dependency*;
- chore: updated Gradle Plugin Publishing Plugin to a just released version without Task.getProject() during execution;

## [0.4.15] - 2025-01-17
- cleanup: minor cleanup
- chore: minor dependency updates
- fix: eliminated calls to Task.getProject() during task execution
- chore: Scala.js dependency update;
- ZioTest dependency update;
- cleanup: changed deprecated `org.junit.Assert.assertThat` to `org.hamcrest.MatcherAssert.assertThat`;
- re-use Gradle's `JUnitOptions` as our TestFrameworkOptions (see the code for the reasons); as a result:
- `useSbt` block now uses `includeCategories`/`excludeCategories` instead of `includeTags`/`excludeTags`
- fix: eliminated calls to Project.afterEvaluate() in Gradle Tasks
- chore: Java 21
- chore: Scala 3.6.3
- chore: latest OpenTorah with Gradle cleanup

## [0.4.14] - 2025-01-07
- fix: OS detection fails in macOS  https://github.com/opentorah/opentorah/issues/341

## [0.4.13] - 2024-12-22
- chore: Gradle 8.12 adjustments and forced update to sbt/zinc
- chore: update ScalaJS
- chore: update test framework versions
- chore: update Scala to 3.6.2
- chore: latest OpenTorah

## [0.4.12] - 2024-06-25
- chore: update test framework versions
- chore: Gradle 8.9 adjustments

## [0.4.11] - 2024-06-02
- chore: Gradle 8.4 adjustments
- chore: update test framework versions
- chore: update ScalaJs
- chore: update Scala to 3.4.2
- chore: update Scala version used in tests  (some frameworks require it)
- chore: Gradle 8.7 adjustments
- chore: Gradle 8.8
- chore: tracking OpenTorah Dependency changes

## [0.4.10] - 2023-12-12
- chore: tracking OpenTorah Dependency changes
- chore: dependency updates
- chore: Gradle 8.3 adjustments

## [0.4.9] - 2023-05-31
- tests: better test testing infrastructure
- cleanup: ScalaJS tutorial and frameworks test projects are now written by the testing infrastructure
- chore: Scala 3.3.0
- chore: ScalaTest 3.2.16
- chore: ScalaJS 1.13.1
- chore: Gradle 8.1.1
- chore: tracking OpenTorah Node changes

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
- cleanup: packaged proxying of the test events in the non-forking scenario into SingleThreadingTestResultProcessor
- cleanup: introduced TestFramework
- cleanup: packaged TestScanner as a TestFrameworkDetector
- cleanup: use Gradle's DefaultTest[Class|Method]Descriptor in test events instead of TaskDefTest
- cleanup: use Gradle's TestMainAction and WorkerTestClassProcessor
- cleanup: boiled down the differences between Gradle's ForkingTestClassProcessor and mine to overridable methods
- cleanup: boiled down the differences between Gradle's TestWorker and mine to overridable methods
- cleanup: boiled down the differences between Gradle's DefaultTestExecuter and mine to overridable methods
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
- docs: documented Node.js version compatibility

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
