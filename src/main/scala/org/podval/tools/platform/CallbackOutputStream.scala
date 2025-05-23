package org.podval.tools.platform

import java.io.OutputStream

// Rewritten from code suggested by the Google Search AI ;)
final class CallbackOutputStream(log: String => Unit) extends OutputStream:
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
    currentLine.setLength(0)  // Clear the buffer
