package org.podval.tools.scalanative

import org.podval.tools.nonjvm.NonJvmBuild
import org.scalajs.linker.interface.LinkingException
import org.slf4j.event.Level
import scala.scalanative.build.{BuildException, Logger as LoggerN}

// see scala.scalanative.sbtplugin.ScalaNativePluginInternal
// https://github.com/scala-native/scala-native/blob/main/sbt-scala-native/src/main/scala/scala/scalanative/sbtplugin/ScalaNativePluginInternal.scala
class ScalaNativeBuild(logSource: String) extends NonJvmBuild(logSource):
  final protected def loggerN: LoggerN = new LoggerN:
    override def trace(throwable: Throwable): Unit = logThrowable(throwable)
    override def debug(msg: String): Unit = logAtLevel(msg, Level.DEBUG)
    override def info (msg: String): Unit = logAtLevel(msg, Level.INFO)
    override def warn (msg: String): Unit = logAtLevel(msg, Level.WARN)
    override def error(msg: String): Unit = logAtLevel(msg, Level.ERROR)

  /** Run `op`, rethrows `BuildException`s as `MessageOnlyException`s. */
  final protected def interceptBuildException[T](op: => T): T =
    try op catch
      case ex: BuildException   => abort(ex.getMessage)
      case ex: LinkingException => abort(ex.getMessage)
