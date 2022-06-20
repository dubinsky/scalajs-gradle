package org.podval.tools.scalajs.testing

import org.gradle.api.logging.Logger
import org.scalajs.jsenv.{Input, JSEnv}
import org.scalajs.logging.Logger as JSLogger
import org.scalajs.testing.adapter.TestAdapter
import sbt.internal.inc.Analysis
import sbt.testing.Framework

// Note: based on sbt.Tests from org.scala-sbt.actions
// Note: based on org.scalajs.sbtplugin.ScalaJSPluginInternal
// Note: based on sbt.Defaults
// TODO report events closer to them happening?
// TODO even with one group, fold the summaries
object Tests:

  def run(
    jsEnv: JSEnv,
    input: Input,
    analysis: Analysis,
    jsLogger: JSLogger,
    logger: TestLogger,
    log: Logger
  ): Unit =
    val listeners: Seq[TestsListener] = Seq(logger)

    val testAdapterConfig: TestAdapter.Config = TestAdapter.Config()
      .withLogger(jsLogger)

    val loadedFrameworks: Seq[Framework] = TestAdapter(
      jsEnv = jsEnv,
      input = Seq(input),
      config = testAdapterConfig
    )
      .loadFrameworks(frameworkNames = TestFramework.all.map(_.implClassNames.toList))
      .flatten

    val frameworkRuns: Seq[FrameworkRun] =
      import collection.mutable

      val map: mutable.Map[Framework, mutable.Set[TestDefinition]] =
        new mutable.HashMap[Framework, mutable.Set[TestDefinition]]

      for
        test: TestDefinition <- Discover.discover(
          fingerprints = loadedFrameworks.flatMap(Util.getFingerprints),
          analysis = analysis,
          log = log
        )
        framework: Framework <- loadedFrameworks.find(Util.isTestForFramework(test, _))
      do
        map.getOrElseUpdate(framework, new mutable.HashSet[TestDefinition]) += test

      for (framework: Framework, tests: mutable.Set[TestDefinition]) <- map.toSeq yield FrameworkRun(
        framework,
        tests.toSet,
        listeners,
        log
      )

    val task: Task[Output] =
      (if frameworkRuns.isEmpty then Task.noop else Task(Util.safeForeach(listeners, log)(_.doInit())))
        .flatMap(_ => TestRunnable.toTasks(frameworkRuns.flatMap(_.testTasks)))
        .map(Output.processResults)
        .flatMap((output: Output) =>
          (if frameworkRuns.isEmpty then Task.noop else Task(Util.safeForeach(listeners, log)(_.doComplete(output.overall))))
            .map(_ => output)
        )

    val output: Output = task.run()

    // getLogger.lifecycle(output.toString)
