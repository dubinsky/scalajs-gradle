package org.podval.tools.build

import org.gradle.api.logging.LogLevel
import org.gradle.process.{ExecOperations, ExecSpec}
import scala.annotation.tailrec
import scala.jdk.CollectionConverters.ListHasAsScala
import java.io.{BufferedReader, InputStream, InputStreamReader, OutputStream}

// Based on org.scalajs.sbtplugin.PipeOutputThread;
// see https://github.com/scala-js/scala-js/blob/main/sbt-plugin/src/main/scala/org/scalajs/sbtplugin/PipeOutputThread.scala

// running on JVM is handled by the Application Plugin, and thus the output is not intercepted by the Runner;
// tests are not handled by the Runner either;
// as a result, println() output gets lost on Scala Native...
final class Runner(
  execOperations: ExecOperations,
  output: Output
):
  private def out(log: Boolean)(message: String): Unit = output.logAtLevel(
    annotation = "out",
    logLevel = if log then LogLevel.LIFECYCLE else LogLevel.INFO,
    message = message
  )

  private def err(message: String): Unit = output.logAtLevel(
    annotation = "err",
    logLevel = LogLevel.ERROR,
    message = message
  )

  def run(running: String, log: Boolean)(body: => Unit): Unit =
    out(log)(s"Running [$running].")
    try body finally
      close()
      // TODO for Native this prints before the output!
      //out(true)(s"Done running [$running].")

  @volatile private var outputStreams: List[OutputStream] = Nil
  
  // this one is for Scala Native and NodeProject;
  def exec(log: Boolean, configure: ExecSpec => Unit): Unit =
    execOperations.exec: (execSpec: ExecSpec) =>
      configure(execSpec)
      run(execSpec.getCommandLine.asScala.mkString(" "), log):
        val outStream: OutputStream = Runner.CallbackOutputStream(out(log))
        val errStream: OutputStream = Runner.CallbackOutputStream(err)
        outputStreams = List(outStream, errStream)
        execSpec.setStandardOutput(outStream)
        execSpec.setErrorOutput   (errStream)

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
    outOpt: Option[InputStream],
    errOpt: Option[InputStream]
  ): Unit =
    pipeOutputThreads =
      Runner.pipeOutputThread(outOpt, out(log = true)) :::
      Runner.pipeOutputThread(errOpt, err)

    pipeOutputThreads.foreach(_.start())

  private def close(): Unit =
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
    
    outputStreams.foreach(_.flush())

object Runner:
  // Rewritten from code suggested by the Google Search AI ;)
  private final class CallbackOutputStream(log: String => Unit) extends OutputStream:
    private val currentLine: StringBuilder = new StringBuilder

    override def write(b: Int): Unit =
      if b == '\n'
      then flushCurrentLine()
      else currentLine.append(b.toChar)

    override def flush(): Unit =
      if currentLine.nonEmpty
      then flushCurrentLine()

    private def flushCurrentLine(): Unit =
      log(currentLine.toString)
      currentLine.setLength(0) // Clear the buffer

  private def pipeOutputThread(
    from: Option[InputStream],
    log: String => Unit
  ): List[Thread] = from.toList.map: (from: InputStream) =>
    val reader: BufferedReader = BufferedReader(InputStreamReader(from))

    @tailrec def loop(): Unit =
      val line: String = reader.readLine()
      if line != null then
        log(line)
        loop()

    new Thread:
      override def run(): Unit = try loop() finally reader.close()
