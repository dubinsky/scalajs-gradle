package org.podval.tools.files

import java.io.{BufferedReader, InputStream, InputStreamReader}
import scala.annotation.tailrec

// Based on org.scalajs.sbtplugin.PipeOutputThread;
// see https://github.com/scala-js/scala-js/blob/main/sbt-plugin/src/main/scala/org/scalajs/sbtplugin/PipeOutputThread.scala
object PipeOutputThread:
  def pipe(from: Option[InputStream], log: String => Unit, prefix: String): List[Thread] =
    from.map(start(_, (line: String) => log(s"$prefix$line"))).toList

  private def start(from: InputStream, log: String => Unit): Thread =
    val thread: PipeOutputThread = PipeOutputThread(
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
