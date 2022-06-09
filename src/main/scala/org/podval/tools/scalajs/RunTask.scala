package org.podval.tools.scalajs

import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.scalajs.jsenv.{Input, JSEnv, JSRun, RunConfig}
import org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv
import org.scalajs.linker.interface.ModuleKind
import org.opentorah.util.Files
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.io.{File, InputStream}
import java.nio.file.Path
import Util.given

// Note: base on org.scalajs.sbtplugin.ScalaJSPluginInternal;
// see https://github.com/scala-js/scala-js/blob/main/sbt-plugin/src/main/scala/org/scalajs/sbtplugin/ScalaJSPluginInternal.scala

abstract class RunTask[T <: LinkTask](clazz: Class[T]) extends AfterLinkTask[T](clazz):
  setDescription(s"Run ScalaJS${stage.description}")
  setGroup("build")

  @TaskAction def execute(): Unit =
    getProject.getLogger.lifecycle(s"Running $path on ${jsEnv.name}\n")

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
      .withLogger(jsLogger)
      .withInheritOut(false)
      .withInheritErr(false)
      .withOnOutputStream((out: Option[InputStream], err: Option[InputStream]) => pipeOutputThreads =
        PipeOutputThread.pipe(out, getProject.getLogger.lifecycle) :::
        PipeOutputThread.pipe(err, getProject.getLogger.error    )
      )

    // TODO Without this delay (or with a shorter one)
    // above log message (and task name logging that comes before it)
    // appear AFTER the output of the run; there should be a better way
    // to ensure that Gradle logging comes first...
    Thread.sleep(2000)

    try
      val run: JSRun = jsEnv.start(Seq(input), config)
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

object RunTask:
  class FastOpt   extends RunTask(classOf[LinkTask.Main.FastOpt  ]) with ScalaJSTask.FastOpt
  class FullOpt   extends RunTask(classOf[LinkTask.Main.FullOpt  ]) with ScalaJSTask.FullOpt
  class Extension extends RunTask(classOf[LinkTask.Main.Extension]) with ScalaJSTask.Extension
