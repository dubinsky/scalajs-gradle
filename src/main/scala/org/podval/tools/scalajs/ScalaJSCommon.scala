package org.podval.tools.scalajs

// TODO disentangle from Gradle - use org.slf4j.Logger?
import org.gradle.api.logging.{Logger, LogLevel as GLevel}
import org.scalajs.linker.interface.ModuleKind as ModuleKindSJS
import org.scalajs.logging.Level as JSLevel
import java.io.File

final class ScalaJSCommon(
  val jsDirectory: File,
  val reportBinFile: File,
  val moduleKind: ModuleKind,
  val logger: Logger,
  val logSource: String,
  val abort: String => Exception
):
  def jsLogger: org.scalajs.logging.Logger = new org.scalajs.logging.Logger:
    override def trace(t: => Throwable): Unit =
      logger.error(s"$logSource Error", t)
    override def log(level: JSLevel, message: => String): Unit =
      logger.log(ScalaJSCommon.scalajs2gradleLevel(level), s"$logSource: $message")

  def moduleKindSJS: ModuleKindSJS = moduleKind match
    case ModuleKind.NoModule       => ModuleKindSJS.NoModule
    case ModuleKind.ESModule       => ModuleKindSJS.ESModule
    case ModuleKind.CommonJSModule => ModuleKindSJS.CommonJSModule

object ScalaJSCommon:
  private given CanEqual[JSLevel, JSLevel] = CanEqual.derived
  private def scalajs2gradleLevel(level: JSLevel): GLevel = level match
    case JSLevel.Error => GLevel.ERROR
    case JSLevel.Warn  => GLevel.WARN
    case JSLevel.Info  => GLevel.INFO
    case JSLevel.Debug => GLevel.DEBUG
