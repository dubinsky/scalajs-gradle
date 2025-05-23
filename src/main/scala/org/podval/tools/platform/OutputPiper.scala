package org.podval.tools.platform

import org.gradle.process.BaseExecSpec
import org.slf4j.{Logger, LoggerFactory}
import java.io.{InputStream, OutputStream}

final class OutputPiper(outputHandler: OutputHandler):
  // this one is for the ExecSpec-based ones: JVM and Scala Native
  def start(exec: BaseExecSpec): Unit =
    exec.setStandardOutput(CallbackOutputStream(outputHandler.out))
    exec.setErrorOutput   (CallbackOutputStream(outputHandler.err))
  
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
  @volatile private var pipeOutputThreads: List[Thread] = Nil

  // this one is for Scala.js
  def start(
    out: Option[InputStream],
    err: Option[InputStream]
  ): Unit =
    pipeOutputThreads =
      PipeOutputThread.pipe(out, outputHandler.out) :::
      PipeOutputThread.pipe(err, outputHandler.err)

  def close(): Unit =
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

object OutputPiper:
  private val logger: Logger = LoggerFactory.getLogger(classOf[OutputPiper])

  def run(
    outputHandler: OutputHandler,
    running: String
  )(
    body: OutputPiper => Unit
  ): Unit =
    logger.info(s"Running $running.")
    val outputPiper: OutputPiper = OutputPiper(outputHandler)
    try
      body(outputPiper)
    finally
      outputPiper.close()
      logger.info(s"Done running $running.")
