package org.podval.tools.scalanative

import org.gradle.api.logging.LogLevel
import org.podval.tools.build.Output
import org.podval.tools.nonjvm.Build
import scala.scalanative.build.Logger as LoggerN

// see scala.scalanative.sbtplugin.ScalaNativePluginInternal
// https://github.com/scala-native/scala-native/blob/main/sbt-scala-native/src/main/scala/scala/scalanative/sbtplugin/ScalaNativePluginInternal.scala
open class ScalaNativeBuild(output: Output) extends Build[LoggerN](output):
  final override protected val interceptedExceptions: Set[Class[? <: Exception]] = Set(
    classOf[scala.scalanative.build.BuildException],
    classOf[org.scalajs.linker.interface.LinkingException]
  )

  final override protected def backendLogger: LoggerN = new LoggerN:
    // TODO running()/time()?
    override def trace(throwable: Throwable): Unit = logThrowable(throwable)
    override def debug(message: String): Unit = logAtLevel(LogLevel.DEBUG, message)
    override def info (message: String): Unit = logAtLevel(LogLevel.INFO , message)
    override def warn (message: String): Unit = logAtLevel(LogLevel.WARN , message)
    override def error(message: String): Unit = logAtLevel(LogLevel.ERROR, message)
