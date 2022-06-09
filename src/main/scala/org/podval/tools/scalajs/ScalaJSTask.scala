package org.podval.tools.scalajs

import org.gradle.api.{DefaultTask, Project}
import org.scalajs.logging.Logger
import org.opentorah.build.Gradle.*

abstract class ScalaJSTask extends DefaultTask:
  protected final def extension: Extension = getProject.getExtension(classOf[Extension])

  protected final def info(message: String): Unit = getProject.getLogger.info(message, null, null, null)

  protected final def jsLogger: Logger = JSLogger(getProject.getLogger, getName)

  protected def stage: Stage

object ScalaJSTask:
  trait FastOpt:
    self: ScalaJSTask =>

    final override protected def stage: Stage = Stage.FastOpt

  trait FullOpt:
    self: ScalaJSTask =>

    final override protected def stage: Stage = Stage.FullOpt

  trait Extension:
    self: ScalaJSTask =>

    final override protected def stage: Stage = extension.stage
