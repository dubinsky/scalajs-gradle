package org.podval.tools.scalajsplugin.scalajs

import org.podval.tools.scalajs.ModuleInitializer
import org.podval.tools.scalajsplugin.nonjvm.NonJvmLinkTask

abstract class ScalaJSLinkTestTask extends NonJvmLinkTask.Test[ScalaJSLinkTask] with ScalaJSLinkTask:
  final override def moduleInitializers: Option[Seq[ModuleInitializer]] = None
