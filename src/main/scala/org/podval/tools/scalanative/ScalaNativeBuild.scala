package org.podval.tools.scalanative

import org.podval.tools.nonjvm.Build
import org.slf4j.event.Level
import scala.scalanative.build.Logger as LoggerN

// see scala.scalanative.sbtplugin.ScalaNativePluginInternal
// https://github.com/scala-native/scala-native/blob/main/sbt-scala-native/src/main/scala/scala/scalanative/sbtplugin/ScalaNativePluginInternal.scala
class ScalaNativeBuild(logSource: String) extends Build[LoggerN](logSource):
  final override protected val interceptedExceptions: Set[Class[? <: Exception]] = Set(
    classOf[scala.scalanative.build.BuildException],
    classOf[org.scalajs.linker.interface.LinkingException]
  )

  final override protected def backendLogger: LoggerN = new LoggerN:
    override def trace(throwable: Throwable): Unit = logThrowable(throwable)
    override def debug(msg: String): Unit = logAtLevel(msg, Level.DEBUG)
    override def info (msg: String): Unit = logAtLevel(msg, Level.INFO )
    override def warn (msg: String): Unit = logAtLevel(msg, Level.WARN )
    override def error(msg: String): Unit = logAtLevel(msg, Level.ERROR)
