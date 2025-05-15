package org.podval.tools.scalajs

import org.podval.tools.files.{OutputHandler, PipeOutputThread}
import org.scalajs.jsenv.{Input, JSEnv, JSRun, RunConfig}
import org.slf4j.{Logger, LoggerFactory}
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import java.io.InputStream

final class ScalaJSRun(runCommon: ScalaJSRunCommon):
  def run(outputHandler: OutputHandler): Unit =
    val jsEnv: JSEnv = runCommon.mkJsEnv
    ScalaJSRun.logger.info(s"Running ${runCommon.mainModulePath} on ${jsEnv.name}.\n")

    /* The list of threads that are piping output to System.out and
     * System.err. This is not an AtomicReference or any other thread-safe
     * structure because:
     * - `onOutputStream` is guaranteed to be called exactly once, and
     * - `pipeOutputThreads` is only read once the run is completed
     *   (although the JSEnv interface does not explicitly specify that the
     *   call to `onOutputStream must happen before that, anything else is
     *   just plain unreasonable).
     * We only mark it as `@volatile` to ensure that there is an
     * appropriate memory barrier between writing to it and reading it back.
     */
    @volatile var pipeOutputThreads: List[Thread] = Nil

    /* #4560 Explicitly redirect out/err to System.out/System.err, instead
     * of relying on `inheritOut` and `inheritErr`, so that streams
     * installed with `System.setOut` and `System.setErr` are always taken
     * into account. sbt installs such alternative outputs when it runs in
     * server mode.
     */
    val config: RunConfig = RunConfig()
      .withLogger(runCommon.common.loggerJS)
      .withInheritOut(false)
      .withInheritErr(false)
      .withOnOutputStream((out: Option[InputStream], err: Option[InputStream]) => pipeOutputThreads =
        PipeOutputThread.pipe(out, outputHandler.out) :::
        PipeOutputThread.pipe(err, outputHandler.err)
      )

    // TODO Without this delay (or with a shorter one)
    // above log message (and task name logging that comes before it)
    // appear AFTER the output of the run; there should be a better way
    // to ensure that Gradle logging comes first...
    Thread.sleep(2000)

    try
      val run: JSRun = jsEnv.start(Seq(runCommon.input), config)
      Await.result(awaitable = run.future, atMost = Duration.Inf)
    finally
      /* Wait for the pipe output threads to be done, to make sure that we
       * do not finish the `run` task before *all* output has been
       * transferred to System.out and System.err.
       * We do that in a `finally` block so that the stdout and stderr
       * streams are propagated even if the run finishes with a failure.
       * `join()` itself does not throw except if the current thread is
       * interrupted, which is not supposed to happen (if it does happen,
       * the interrupted exception will shadow any error from the run).
       */
      pipeOutputThreads.foreach(_.join)

object ScalaJSRun:
  private val logger: Logger = LoggerFactory.getLogger(ScalaJSRun.getClass)
  