package org.podval.tools.scalajs

import java.io.{BufferedReader, InputStream, InputStreamReader}
import scala.annotation.tailrec

// Note: based on org.scalajs.sbtplugin.PipeOutputThread;
// see https://github.com/scala-js/scala-js/blob/main/sbt-plugin/src/main/scala/org/scalajs/sbtplugin/PipeOutputThread.scala

object PipeOutputThread:
  def pipe(from: Option[InputStream], log: String => Unit): List[Thread] =
    from.map(start(_, log)).toList

  private def start(from: InputStream, log: String => Unit): Thread =
    val thread: PipeOutputThread = new PipeOutputThread(
      BufferedReader(InputStreamReader(from)),
      log
    )
    thread.start()
    thread

private final class PipeOutputThread(from: BufferedReader, log: String => Unit) extends Thread:
  override def run(): Unit =
    try
      @tailrec def loop(): Unit =
        val line: String = from.readLine()
        if line != null then
          log(line)
          loop()

      loop()
    finally
      from.close()
