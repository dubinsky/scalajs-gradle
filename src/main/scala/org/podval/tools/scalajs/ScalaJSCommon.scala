package org.podval.tools.scalajs

import org.slf4j.{Logger, LoggerFactory}
import org.scalajs.linker.interface.ModuleKind as ModuleKindSJS
import org.scalajs.logging.{Level as LevelJS, Logger as LoggerJS}
import java.io.File

final class ScalaJSCommon(
  val jsDirectory: File,
  val reportBinFile: File,
  val moduleKind: ModuleKind,
  val logSource: String,
  val abort: String => Exception
):
  private val logger: Logger = LoggerFactory.getLogger(classOf[ScalaJSCommon])
  
  def loggerJS: LoggerJS = new LoggerJS:
    override def trace(t: => Throwable): Unit =
      logger.error(s"$logSource Error", t)

    override def log(level: LevelJS, message: => String): Unit =
      val toLog: String = s"$logSource: $message"

      // Gradle has its own copy of org.slf4j API, which predates introduction of logger.atLevel().
      given CanEqual[LevelJS, LevelJS] = CanEqual.derived
      level match
        case LevelJS.Error => logger.error(toLog)
        case LevelJS.Warn  => logger.warn (toLog)
        case LevelJS.Info  => logger.info (toLog)
        case LevelJS.Debug => logger.debug(toLog)

  def moduleKindSJS: ModuleKindSJS = moduleKind match
    case ModuleKind.NoModule       => ModuleKindSJS.NoModule
    case ModuleKind.ESModule       => ModuleKindSJS.ESModule
    case ModuleKind.CommonJSModule => ModuleKindSJS.CommonJSModule

